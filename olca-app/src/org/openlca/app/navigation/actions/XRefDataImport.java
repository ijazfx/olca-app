package org.openlca.app.navigation.actions;

import java.io.File;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.App;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.io.refdata.RefDataImport;

class XRefDataImport extends Action implements INavigationAction {

	public XRefDataImport() {
		setImageDescriptor(Icon.EXTENSION.descriptor());
		setText("Import reference data");
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof DatabaseElement))
			return false;
		DatabaseElement e = (DatabaseElement) element;
		return Database.isActive(e.getContent());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

	@Override
	public void run() {
		File dir = FileChooser.openFolder();
		if (dir == null)
			return;
		Cache.evictAll();
		RefDataImport refImport = new RefDataImport(dir, Database.get());
		App.run("Import reference data", refImport, () -> {
			Cache.evictAll();
			Navigator.refresh();
		});
	}

}
