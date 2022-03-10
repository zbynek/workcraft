package org.workcraft.plugins.petri.utils;

import org.workcraft.dom.visual.Replica;
import org.workcraft.dom.visual.VisualModel;
import org.workcraft.dom.visual.VisualNode;
import org.workcraft.dom.visual.connections.VisualConnection;
import org.workcraft.plugins.petri.VisualPlace;
import org.workcraft.plugins.petri.VisualReadArc;
import org.workcraft.plugins.petri.VisualReplicaPlace;
import org.workcraft.plugins.petri.VisualTransition;
import org.workcraft.utils.Hierarchy;

import java.util.HashSet;

public class ConnectionUtils extends org.workcraft.dom.visual.connections.ConnectionUtils {

    public static boolean hasReadArcConnection(VisualModel visualModel, VisualNode first, VisualNode second) {
        VisualPlace place = getVisualPlaceOrNull(first);
        VisualTransition transition = getVisualTransitionOrNull(second);
        if ((place == null) || (transition == null)) {
            return false;
        }
        for (Replica replica : place.getReplicas()) {
            if (replica instanceof VisualReplicaPlace) {
                VisualReplicaPlace replicaPlace = (VisualReplicaPlace) replica;
                VisualConnection connection = visualModel.getConnection(replicaPlace, transition);
                if (connection instanceof VisualReadArc) {
                    return true;
                }
            }
        }
        VisualConnection connection = visualModel.getConnection(place, transition);
        return (connection instanceof VisualReadArc);
    }

    public static boolean hasProducingArcConnection(VisualModel visualModel, VisualNode first, VisualNode second) {
        VisualPlace place = getVisualPlaceOrNull(second);
        VisualTransition transition = getVisualTransitionOrNull(first);
        if ((place == null) || (transition == null)) {
            return false;
        }
        for (Replica replica : place.getReplicas()) {
            if (replica instanceof VisualReplicaPlace) {
                VisualReplicaPlace replicaPlace = (VisualReplicaPlace) replica;
                VisualConnection connection = visualModel.getConnection(transition, replicaPlace);
                if ((connection != null) && !(connection instanceof VisualReadArc)) {
                    return true;
                }
            }
        }
        VisualConnection connection = visualModel.getConnection(transition, place);
        return (connection != null) && !(connection instanceof VisualReadArc);
    }

    public static boolean hasConsumingArcConnection(VisualModel visualModel, VisualNode first, VisualNode second) {
        VisualPlace place = getVisualPlaceOrNull(first);
        VisualTransition transition = getVisualTransitionOrNull(second);
        if ((place == null) || (transition == null)) {
            return false;
        }
        for (Replica replica : place.getReplicas()) {
            if (replica instanceof VisualReplicaPlace) {
                VisualReplicaPlace replicaPlace = (VisualReplicaPlace) replica;
                VisualConnection connection = visualModel.getConnection(replicaPlace, transition);
                if ((connection != null) && !(connection instanceof VisualReadArc)) {
                    return true;
                }
            }
        }
        VisualConnection connection = visualModel.getConnection(place, transition);
        return (connection != null) && !(connection instanceof VisualReadArc);
    }

    private static VisualPlace getVisualPlaceOrNull(VisualNode node) {
        if (node instanceof VisualPlace) {
            return (VisualPlace) node;
        } else if (node instanceof VisualReplicaPlace) {
            VisualReplicaPlace r = (VisualReplicaPlace) node;
            return  (VisualPlace) r.getMaster();
        }
        return null;
    }

    private static VisualTransition getVisualTransitionOrNull(VisualNode node) {
        return (node instanceof VisualTransition) ? (VisualTransition) node : null;
    }

    public static HashSet<VisualConnection> getVisualConsumingArcs(VisualModel visualModel) {
        HashSet<VisualConnection> connections = new HashSet<>();
        for (VisualConnection connection : Hierarchy.getDescendantsOfType(visualModel.getRoot(), VisualConnection.class)) {
            if (isVisualConsumingArc(connection)) {
                connections.add(connection);
            }
        }
        return connections;
    }

    public static HashSet<VisualConnection> getVisualProducingArcs(VisualModel visualModel) {
        HashSet<VisualConnection> connections = new HashSet<>();
        for (VisualConnection connection : Hierarchy.getDescendantsOfType(visualModel.getRoot(), VisualConnection.class)) {
            if (isVisualProducingArc(connection)) {
                connections.add(connection);
            }
        }
        return connections;
    }

    public static HashSet<VisualReadArc> getVisualReadArcs(VisualModel visualModel) {
        return new HashSet<>(Hierarchy.getDescendantsOfType(visualModel.getRoot(), VisualReadArc.class));
    }

    public static HashSet<VisualReplicaPlace> getVisualReplicaPlaces(VisualModel visualModel) {
        return new HashSet<>(Hierarchy.getDescendantsOfType(visualModel.getRoot(), VisualReplicaPlace.class));
    }

    public static boolean isVisualProducingArc(VisualNode node) {
        if ((node instanceof VisualConnection) && !(node instanceof VisualReadArc)) {
            VisualConnection connection = (VisualConnection) node;
            return (connection.getFirst() instanceof VisualTransition)
                    && ((connection.getSecond() instanceof VisualPlace)
                    || (connection.getSecond() instanceof VisualReplicaPlace));
        }
        return false;
    }

    public static boolean isVisualConsumingArc(VisualNode node) {
        if ((node instanceof VisualConnection) && !(node instanceof VisualReadArc)) {
            VisualConnection connection = (VisualConnection) node;
            return ((connection.getFirst() instanceof VisualPlace)
                    || (connection.getFirst() instanceof VisualReplicaPlace))
                    && (connection.getSecond() instanceof VisualTransition);
        }
        return false;
    }

}
