package org.workcraft.plugins.petrify.commands;

import java.util.LinkedList;

import org.workcraft.Framework;
import org.workcraft.gui.graph.commands.AbstractSynthesisCommand;
import org.workcraft.plugins.petrify.tasks.PetrifySynthesisResultHandler;
import org.workcraft.plugins.petrify.tasks.PetrifySynthesisTask;
import org.workcraft.plugins.stg.Mutex;
import org.workcraft.plugins.stg.MutexUtils;
import org.workcraft.plugins.stg.Stg;
import org.workcraft.plugins.stg.StgModel;
import org.workcraft.tasks.TaskManager;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.workspace.WorkspaceUtils;

public abstract class PetrifyAbstractSynthesisCommand extends AbstractSynthesisCommand {

    @Override
    public boolean isApplicableTo(WorkspaceEntry we) {
        return WorkspaceUtils.isApplicable(we, StgModel.class);
    }

    @Override
    public WorkspaceEntry execute(WorkspaceEntry we) {
        Stg stg = WorkspaceUtils.getAs(we, Stg.class);
        LinkedList<Mutex> mutexes = MutexUtils.getImplementableMutexes(stg);
        if (mutexes == null) {
            return null;
        }
        MutexUtils.logInfoPossiblyImplementableMutex(mutexes);
        final Framework framework = Framework.getInstance();
        final TaskManager taskManager = framework.getTaskManager();
        final PetrifySynthesisTask task = new PetrifySynthesisTask(we, getSynthesisParameter(), mutexes);
        final PetrifySynthesisResultHandler monitor = new PetrifySynthesisResultHandler(we,
                boxSequentialComponents(), boxCombinationalComponents(), sequentialAssign(), mutexes);

        taskManager.execute(task, "Petrify logic synthesis", monitor);
        return monitor.getResult();
    }

    @Override
    public void run(WorkspaceEntry we) {
        Stg stg = WorkspaceUtils.getAs(we, Stg.class);
        LinkedList<Mutex> mutexes = MutexUtils.getImplementableMutexes(stg);
        if (mutexes == null) {
            return;
        }
        MutexUtils.logInfoPossiblyImplementableMutex(mutexes);
        final Framework framework = Framework.getInstance();
        final TaskManager taskManager = framework.getTaskManager();
        final PetrifySynthesisTask task = new PetrifySynthesisTask(we, getSynthesisParameter(), mutexes);
        final PetrifySynthesisResultHandler monitor = new PetrifySynthesisResultHandler(we,
                boxSequentialComponents(), boxCombinationalComponents(), sequentialAssign(), mutexes);

        taskManager.queue(task, "Petrify logic synthesis", monitor);
    }

    public boolean boxSequentialComponents() {
        return false;
    }

    public boolean boxCombinationalComponents() {
        return false;
    }

    public boolean sequentialAssign() {
        return false;
    }

    public abstract String[] getSynthesisParameter();

}
