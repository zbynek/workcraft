package org.workcraft.plugins.stg.tools;

import org.workcraft.Framework;
import org.workcraft.dom.hierarchy.NamespaceHelper;
import org.workcraft.dom.math.MathNode;
import org.workcraft.dom.visual.SizeHelper;
import org.workcraft.dom.visual.VisualComponent;
import org.workcraft.dom.visual.VisualModel;
import org.workcraft.dom.visual.VisualNode;
import org.workcraft.dom.visual.connections.VisualConnection;
import org.workcraft.gui.controls.FlatHeaderRenderer;
import org.workcraft.gui.properties.BooleanCellEditor;
import org.workcraft.gui.properties.BooleanCellRenderer;
import org.workcraft.gui.properties.ColorCellEditor;
import org.workcraft.gui.properties.ColorCellRenderer;
import org.workcraft.gui.tools.Decoration;
import org.workcraft.gui.tools.GraphEditor;
import org.workcraft.plugins.builtin.settings.SignalCommonSettings;
import org.workcraft.plugins.builtin.settings.SimulationDecorationSettings;
import org.workcraft.plugins.dtd.DtdDescriptor;
import org.workcraft.plugins.dtd.VisualDtd;
import org.workcraft.plugins.petri.Transition;
import org.workcraft.plugins.petri.VisualPlace;
import org.workcraft.plugins.petri.VisualTransition;
import org.workcraft.plugins.petri.tools.PetriSimulationTool;
import org.workcraft.plugins.petri.tools.PlaceDecoration;
import org.workcraft.plugins.stg.*;
import org.workcraft.plugins.stg.converters.StgToDtdConverter;
import org.workcraft.plugins.stg.converters.StgToStgConverter;
import org.workcraft.plugins.stg.utils.LabelParser;
import org.workcraft.plugins.stg.utils.StgUtils;
import org.workcraft.shared.ColorGenerator;
import org.workcraft.traces.Trace;
import org.workcraft.types.Pair;
import org.workcraft.utils.*;
import org.workcraft.workspace.ModelEntry;
import org.workcraft.workspace.WorkspaceEntry;

import javax.activation.ActivationDataFlavor;
import javax.activation.DataHandler;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DragSource;
import java.util.List;
import java.util.*;

public class StgSimulationTool extends PetriSimulationTool {

    private static final int COLUMN_SIGNAL = 0;
    private static final int COLUMN_STATE = 1;
    private static final int COLUMN_VISIBLE = 2;
    private static final int COLUMN_COLOR = 3;
    private static final String GRAY_CODE = ColorUtils.getHexRGB(Color.GRAY);

    protected HashMap<String, SignalData> signalDataMap = new HashMap<>();
    protected LinkedList<String> signals = new LinkedList<>();
    protected JTable stateTable;
    private JPanel panel;
    private HashMap<String, Boolean> initialSignalState = new HashMap<>();
    private StgToStgConverter converter;
    private Map<String, MathNode> refToUnderlyingNodeMap;

    public StgSimulationTool() {
        super(true);
    }

    public static final class SignalData {
        public final String name;
        public final Signal.Type type;

        public Signal.State value = Signal.State.UNDEFINED;
        public boolean excited = false;
        public Boolean visible;
        public Color color = Color.BLACK;

        public SignalData(String name, Signal.Type type) {
            this.name = name;
            this.type = type;
            visible = type != Signal.Type.INTERNAL;
        }

    }

    private final class StateTable extends JTable {
        StateTable(StateTableModel model) {
            super(model);
            getTableHeader().setDefaultRenderer(new FlatHeaderRenderer());
            getTableHeader().setReorderingAllowed(false);
            setDragEnabled(true);
            setDropMode(DropMode.INSERT_ROWS);
            setTransferHandler(new StateTableRowTransferHandler(this));
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            setRowHeight(SizeHelper.getComponentHeightFromFont(this.getFont()));
            setDefaultRenderer(SignalData.class, new SignalDataRenderer());
            setDefaultRenderer(Boolean.class, new BooleanCellRenderer());
            setDefaultEditor(Boolean.class, new BooleanCellEditor());
            setDefaultRenderer(Color.class, new ColorCellRenderer());
            setDefaultEditor(Color.class, new ColorCellEditor());
        }

        @Override
        public void editingStopped(ChangeEvent e) {
            TableCellEditor cellEditor = getCellEditor();
            String signalName = signals.get(editingRow);
            if ((cellEditor != null) && (signalName != null)) {
                SignalData signalData = signalDataMap.get(signalName);
                Object value = cellEditor.getCellEditorValue();
                if ((signalData != null) && (value != null)) {
                    switch (editingColumn) {
                    case COLUMN_VISIBLE:
                        signalData.visible = (Boolean) value;
                        break;
                    case COLUMN_COLOR:
                        signalData.color = (Color) value;
                        break;
                    }
                    setValueAt(value, editingRow, editingColumn);
                    removeEditor();
                }
            }
        }
    }

    private final class StateTableModel extends AbstractTableModel {

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
            case COLUMN_SIGNAL:
                return "Signal";
            case COLUMN_STATE:
                return "State";
            case COLUMN_VISIBLE:
                return "Visible";
            case COLUMN_COLOR:
                return "Color";
            default:
                return null;
            }
        }

        @Override
        public Class<?> getColumnClass(int col) {
            switch (col) {
            case COLUMN_SIGNAL:
            case COLUMN_STATE:
                return SignalData.class;
            case COLUMN_VISIBLE:
                return Boolean.class;
            case COLUMN_COLOR:
                return Color.class;
            default:
                return null;
            }
        }

        @Override
        public int getRowCount() {
            return (signalDataMap == null) ? 0 : signalDataMap.size();
        }

        @Override
        public Object getValueAt(int row, int col) {
            if (row < signalDataMap.size()) {
                String signalName = signals.get(row);
                SignalData signalData = signalDataMap.get(signalName);
                if (signalData != null) {
                    switch (col) {
                    case COLUMN_SIGNAL:
                    case COLUMN_STATE:
                        return signalData;
                    case COLUMN_VISIBLE:
                        return signalData.visible;
                    case COLUMN_COLOR:
                        return signalData.color;
                    default:
                        return null;
                    }
                }
            }
            return null;
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            switch (col) {
            case COLUMN_VISIBLE:
            case COLUMN_COLOR:
                return true;
            default:
                return false;
            }
        }

        public void reorderRows(int from, int to) {
            if ((from >= 0) && (from < signals.size()) && (to >= 0) && (to < signals.size()) && (from != to)) {
                String name = signals.remove(from);
                signals.add(to, name);
                fireTableDataChanged();
            }
        }
    }

    private final class SignalDataRenderer implements TableCellRenderer {

        private final JLabel label = new JLabel() {
            @Override
            public void paint(Graphics g) {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paint(g);
            }
        };

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int col) {

            label.setText("");
            label.setBorder(GuiUtils.getTableCellBorder());
            label.setForeground(table.getForeground());
            label.setBackground(table.getBackground());
            label.setFont(table.getFont().deriveFont(Font.PLAIN));
            if (isActivated() && (value instanceof SignalData)) {
                SignalData signalData = (SignalData) value;
                switch (col) {
                case COLUMN_SIGNAL:
                    label.setText(signalData.name);
                    label.setForeground(getTypeColor(signalData.type));
                    break;
                case COLUMN_STATE:
                    label.setText(signalData.value.toString());
                    if (signalData.excited) {
                        label.setFont(table.getFont().deriveFont(Font.BOLD));
                    }
                    break;
                default:
                    break;
                }
            }

            boolean fits = GuiUtils.getLabelTextWidth(label) < GuiUtils.getTableColumnTextWidth(table, col);
            label.setToolTipText(fits ? null : label.getText());
            return label;
        }
    }

    public class StateTableRowTransferHandler extends TransferHandler {

        private final DataFlavor localObjectFlavor = new ActivationDataFlavor(Integer.class,
                "Integer Row Index");

        private final JTable table;

        public StateTableRowTransferHandler(JTable table) {
            this.table = table;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            return new DataHandler(table.getSelectedRow(), localObjectFlavor.getMimeType());
        }

        @Override
        public boolean canImport(TransferHandler.TransferSupport info) {
            boolean result = (info.getComponent() == table) && info.isDrop() && info.isDataFlavorSupported(localObjectFlavor);
            table.setCursor(result ? DragSource.DefaultMoveDrop : DragSource.DefaultMoveNoDrop);
            return result;
        }

        @Override
        public int getSourceActions(JComponent c) {
            return TransferHandler.COPY_OR_MOVE;
        }

        @Override
        public boolean importData(TransferHandler.TransferSupport info) {
            JTable target = (JTable) info.getComponent();
            JTable.DropLocation dl = (JTable.DropLocation) info.getDropLocation();
            int toRow = dl.getRow();
            int lastRow = table.getModel().getRowCount();
            if ((toRow < 0) || (toRow > lastRow)) {
                toRow = lastRow;
            }
            target.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            try {
                int fromRow = (Integer) info.getTransferable().getTransferData(localObjectFlavor);
                if (toRow > fromRow) {
                    toRow--;
                }
                if ((fromRow != -1) && (fromRow != toRow)) {
                    StateTableModel stateTableModel = (StateTableModel) table.getModel();
                    stateTableModel.reorderRows(fromRow, toRow);
                    target.getSelectionModel().addSelectionInterval(toRow, toRow);
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected void exportDone(JComponent c, Transferable t, int act) {
            if ((act == TransferHandler.MOVE) || (act == TransferHandler.NONE)) {
                table.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        }

    }

    private final class TraceTableCellRendererImplementation implements TableCellRenderer {

        private final JLabel label = new JLabel() {
            @Override
            public void paint(Graphics g) {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth() - 1, getHeight() - 1);
                super.paint(g);
            }
        };

        private boolean isActive(int row, int column) {
            if (column == 0) {
                if (!mainTrace.isEmpty() && branchTrace.isEmpty()) {
                    return row == mainTrace.getPosition();
                }
            } else {
                int absoluteBranchSize = mainTrace.getPosition() + branchTrace.size();
                int absoluteBranchPosition = mainTrace.getPosition() + branchTrace.getPosition();
                if (!branchTrace.isEmpty() && (row >= mainTrace.getPosition()) && (row < absoluteBranchSize)) {
                    return row == absoluteBranchPosition;
                }
            }
            return false;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            JLabel result = null;
            label.setBorder(GuiUtils.getTableCellBorder());
            if (isActivated() && (value instanceof String)) {
                String text = value.toString();
                Pair<String, String> pair = TraceUtils.splitLoopDecoration(text);
                String prefix = pair.getFirst();
                String ref = pair.getSecond();
                MathNode node = getUnderlyingNode(ref);
                String colorCode = ColorUtils.getHexRGB(getNodeColor(node));
                label.setText("<html><span style='color: " + GRAY_CODE + "'>" + prefix + "</span>" +
                        "<span style='color: " + colorCode + "'>" + ref + "</span></html>");

                if (isActive(row, column)) {
                    label.setBackground(table.getSelectionBackground());
                } else {
                    label.setBackground(table.getBackground());
                }
                result = label;
            }
            return result;
        }
    }

    @Override
    public void generateUnderlyingModel(WorkspaceEntry we) {
        VisualStg srcStg = WorkspaceUtils.getAs(we, VisualStg.class);
        converter = new StgToStgConverter(srcStg);

        refToUnderlyingNodeMap = new HashMap<>();
        for (VisualTransition srcTransition : srcStg.getVisualTransitions()) {
            String srcRef = srcStg.getMathReference(srcTransition);
            VisualNode dstNode = converter.getSrcToDstNode(srcTransition);
            if (dstNode instanceof VisualComponent) {
                VisualComponent dstComponent = (VisualComponent) dstNode;
                refToUnderlyingNodeMap.put(srcRef, dstComponent.getReferencedComponent());
            }
        }
    }

    @Override
    public Stg getUnderlyingModel() {
        return converter.getDstModel().getMathModel();
    }

    @Override
    public MathNode getUnderlyingNode(String ref) {
        MathNode result = null;
        if (refToUnderlyingNodeMap != null) {
            result = refToUnderlyingNodeMap.get(ref);
        }
        if (result == null) {
            result = super.getUnderlyingNode(ref);
        }
        return result;
    }

    @Override
    public VisualModel getUnderlyingVisualModel() {
        return converter.getDstModel();
    }

    @Override
    public JPanel getControlsPanel(GraphEditor editor) {
        if (panel == null) {
            panel = super.getControlsPanel(editor);
            stateTable = new StateTable(new StateTableModel());
            statePane.setViewportView(stateTable);
            traceTable.setDefaultRenderer(Object.class, new TraceTableCellRendererImplementation());
        }
        return panel;
    }

    @Override
    public void updateState(GraphEditor editor) {
        super.updateState(editor);
        updateSignalState();
        stateTable.tableChanged(new TableModelEvent(stateTable.getModel()));
    }

    public void updateSignalState() {
        initialiseSignalState();
        ArrayList<String> combinedTrace = new ArrayList<>();
        if (!mainTrace.isEmpty()) {
            combinedTrace.addAll(mainTrace.subList(0, mainTrace.getPosition()));
        }
        if (!branchTrace.isEmpty()) {
            combinedTrace.addAll(branchTrace.subList(0, branchTrace.getPosition()));
        }

        for (String ref : combinedTrace) {
            MathNode node = getUnderlyingNode(ref);
            if (node instanceof SignalTransition) {
                SignalTransition transition = (SignalTransition) node;
                String signalReference = getUnderlyingModel().getSignalReference(transition);
                SignalData signalState = signalDataMap.get(signalReference);
                if (signalState != null) {
                    switch (transition.getDirection()) {
                    case MINUS:
                        signalState.value = Signal.State.LOW;
                        break;
                    case PLUS:
                        signalState.value = Signal.State.HIGH;
                        break;
                    case TOGGLE:
                        signalState.value = signalState.value.toggle();
                        break;
                    default:
                        break;
                    }
                }
            }
        }

        for (SignalTransition transition : getUnderlyingModel().getSignalTransitions()) {
            String signalReference = getUnderlyingModel().getSignalReference(transition);
            SignalData signalData = signalDataMap.get(signalReference);
            if (signalData != null) {
                signalData.excited |= isEnabledUnderlyingNode(transition);
            }
        }
    }

    public void initialiseSignalState() {
        for (String signal : signalDataMap.keySet()) {
            SignalData signalData = signalDataMap.get(signal);
            Boolean signalState = (initialSignalState == null) ? null : initialSignalState.get(signal);
            if (signalState == null) {
                signalData.value = Signal.State.UNDEFINED;
            } else {
                signalData.value = signalState ? Signal.State.HIGH : Signal.State.LOW;
            }
            signalData.excited = false;
        }
    }

    @Override
    public void activated(GraphEditor editor) {
        super.activated(editor);
        initialSignalState = getInitialState();
        initialiseStateMap();
        setStatePaneVisibility(true);
    }

    private HashMap<String, Boolean> getInitialState() {
        Stg stg = getUnderlyingModel();
        HashMap<String, Boolean> result = StgUtils.guessInitialStateFromSignalPlaces(stg);
        Set<String> signalRefs = stg.getSignalReferences();
        if (result.size() < signalRefs.size()) {
            result = StgUtils.getInitialState(stg, 500);
        }
        return result;
    }

    @Override
    public void generateTraceGraph(GraphEditor editor) {
        Trace trace = getCombinedTrace();
        if (trace.isEmpty()) {
            DialogUtils.showWarning("Cannot generate a timing diagram for an empty trace.");
        } else {
            Stg stg = getUnderlyingModel();
            LinkedList<Pair<String, Color>> visibleSignals = getVisibleSignals();
            StgToDtdConverter converter = new StgToDtdConverter(stg, trace, visibleSignals);
            VisualDtd dtd = converter.getVisualDtd();
            ModelEntry me = new ModelEntry(new DtdDescriptor(), dtd);
            Framework framework = Framework.getInstance();
            framework.createWork(me, editor.getWorkspaceEntry().getFileName());
        }
    }

    private LinkedList<Pair<String, Color>> getVisibleSignals() {
        LinkedList<Pair<String, Color>> result = new LinkedList<>();
        for (String signalRef : signals) {
            SignalData signalData = signalDataMap.get(signalRef);
            if ((signalData != null) && signalData.visible) {
                result.add(Pair.of(signalData.name, signalData.color));
            }
        }
        return result;
    }

    @Override
    public String getTraceLabelByReference(String ref) {
        String result = ref;
        if (ref != null) {
            String name = NamespaceHelper.getReferenceName(ref);
            Pair<String, Integer> instancedTransition = LabelParser.parseInstancedTransition(name);
            if (instancedTransition != null) {
                String parentRef = NamespaceHelper.getParentReference(ref);
                result = NamespaceHelper.getReference(parentRef, instancedTransition.getFirst());
            }
        }
        return result;
    }

    private void initialiseStateMap() {
        Stg stg = getUnderlyingModel();
        HashMap<String, SignalData> newStateMap = new HashMap<>();
        List<String> allSignals = new LinkedList<>();
        for (Signal.Type type : Signal.Type.values()) {
            List<String> typedSignals = new LinkedList<>(stg.getSignalReferences(type));
            SortUtils.sortNatural(typedSignals);
            allSignals.addAll(typedSignals);
            for (String signal : typedSignals) {
                SignalData signalData = signalDataMap.getOrDefault(signal, new SignalData(signal, type));
                newStateMap.put(signal, signalData);
            }
        }
        signalDataMap = newStateMap;
        // Preserve "old" and append "new" items of allSignals to signals list.
        signals.retainAll(allSignals);
        allSignals.removeAll(signals);
        signals.addAll(allSignals);
        updateSignalState();
    }

    private Color getNodeColor(MathNode node) {
        if (node instanceof SignalTransition) {
            SignalTransition transition = (SignalTransition) node;
            return getTypeColor(transition.getSignalType());
        }
        return Color.BLACK;
    }

    private Color getTypeColor(Signal.Type type) {
        switch (type) {
        case INPUT:    return SignalCommonSettings.getInputColor();
        case OUTPUT:   return SignalCommonSettings.getOutputColor();
        case INTERNAL: return SignalCommonSettings.getInternalColor();
        default:       return SignalCommonSettings.getDummyColor();
        }
    }

    @Override
    public void coloriseTokens(Transition t) {
        VisualModel model = getUnderlyingVisualModel();
        VisualStg stg = (model instanceof VisualStg) ? (VisualStg) model : null;
        if (stg == null) {
            return;
        }

        VisualTransition vt = stg.getVisualTransition(t);
        if (vt == null) {
            return;
        }

        Color tokenColor = Color.BLACK;
        ColorGenerator tokenColorGenerator = vt.getTokenColorGenerator();
        if (tokenColorGenerator != null) {
            // Generate token colour
            tokenColor = tokenColorGenerator.updateColor();
        } else {
            // Combine preset token colours
            for (VisualConnection vc : stg.getConnections(vt)) {
                if ((vc.getSecond() == vt) && vc.isTokenColorPropagator()) {
                    if (vc.getFirst() instanceof VisualPlace) {
                        VisualPlace vp = (VisualPlace) vc.getFirst();
                        tokenColor = ColorUtils.colorise(tokenColor, vp.getTokenColor());
                    } else if (vc instanceof VisualImplicitPlaceArc) {
                        VisualImplicitPlaceArc vipa = (VisualImplicitPlaceArc) vc;
                        tokenColor = ColorUtils.colorise(tokenColor, vipa.getTokenColor());
                    }
                }
            }
        }
        // Propagate the colour to postset tokens
        for (VisualConnection vc : stg.getConnections(vt)) {
            if ((vc.getFirst() == vt) && vc.isTokenColorPropagator()) {
                if (vc.getSecond() instanceof VisualPlace) {
                    VisualPlace vp = (VisualPlace) vc.getSecond();
                    vp.setTokenColor(tokenColor);
                } else if (vc instanceof VisualImplicitPlaceArc) {
                    VisualImplicitPlaceArc vipa = (VisualImplicitPlaceArc) vc;
                    vipa.setTokenColor(tokenColor);
                }
            }
        }
    }

    @Override
    public Decoration getConnectionDecoration(VisualModel model, VisualConnection connection) {
        VisualImplicitPlaceArc underlyingVisualImplicitArc = getUnderlyingVisualImplicitArc(model, connection);
        if (underlyingVisualImplicitArc == null) {
            return super.getConnectionDecoration(model, connection);
        }
        StgPlace underlyingPlace = underlyingVisualImplicitArc.getImplicitPlace();

        return new PlaceDecoration() {
            @Override
            public Color getColorisation() {
                return underlyingPlace.isImplicit() & (underlyingPlace.getTokens() > 0)
                        ? SimulationDecorationSettings.getExcitedComponentColor() : null;
            }

            @Override
            public Color getBackground() {
                return null;
            }

            @Override
            public int getTokens() {
                return underlyingPlace.getTokens();
            }

            @Override
            public Color getTokenColor() {
                return underlyingVisualImplicitArc.getTokenColor();
            }
        };
    }

    private VisualImplicitPlaceArc getUnderlyingVisualImplicitArc(VisualModel model, VisualConnection connection) {
        VisualModel underlyingVisualModel = getUnderlyingVisualModel();
        if ((connection instanceof VisualImplicitPlaceArc) && (underlyingVisualModel instanceof VisualStg)) {
            String ref = model.getMathReference(connection);
            VisualStg underlyingVisualStg = (VisualStg) underlyingVisualModel;
            for (VisualImplicitPlaceArc underlyingVisualArc : underlyingVisualStg.getVisualImplicitPlaceArcs()) {
                if (ref.equals(underlyingVisualModel.getMathReference(underlyingVisualArc))) {
                    return underlyingVisualArc;
                }
            }
        }
        return null;
    }

}
