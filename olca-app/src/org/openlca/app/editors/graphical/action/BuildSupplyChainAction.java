package org.openlca.app.editors.graphical.action;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.openlca.app.Messages;
import org.openlca.app.db.Cache;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.util.UI;
import org.openlca.core.database.IProductSystemBuilder;
import org.openlca.core.matrix.LongPair;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.ProductSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BuildSupplyChainAction extends Action implements IBuildAction {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private ProcessNode node;
	private ProcessType preferredType = ProcessType.UNIT_PROCESS;

	BuildSupplyChainAction() {
		setId(ActionIds.BUILD_SUPPLY_CHAIN);
		setText(Messages.Complete);
	}

	@Override
	public void setProcessNode(ProcessNode node) {
		this.node = node;
	}

	void setPreferredType(ProcessType preferredType) {
		this.preferredType = preferredType;
	}

	@Override
	public void run() {
		try {
			if (node.getParent().getEditor().promptSaveIfNecessary())
				new ProgressMonitorDialog(UI.shell()).run(true, false,
						new Runner());
		} catch (final Exception e) {
			log.error("Failed to complete product system. ", e);
		}
		node.getParent().getEditor().reload();
	}

	private class Runner implements IRunnableWithProgress {

		@Override
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			monitor.beginTask(Messages.Systems_CreatingProductSystem,
					IProgressMonitor.UNKNOWN);
			ProductSystem system = node.getParent().getProductSystem();
			LongPair idPair = new LongPair(node.getProcess().getId(), node
					.getProcess().getQuantitativeReference());
			IProductSystemBuilder.Factory.create(Cache.getMatrixCache(),
					preferredType == ProcessType.LCI_RESULT).autoComplete(
					system, idPair);
		}
	}

}
