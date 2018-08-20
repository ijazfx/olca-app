package org.openlca.app.editors.processes.exchanges;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.widgets.TableItem;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.util.tables.TableClipboard;
import org.openlca.core.database.FlowDao;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.Uncertainty;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.openlca.io.CategoryPath;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Clipboard {

	static String converter(TableItem item, int col) {
		if (item == null)
			return "";
		if (col != 2)
			return TableClipboard.text(item, col);
		Object data = item.getData();
		if (!(data instanceof Exchange))
			return TableClipboard.text(item, col);
		Exchange e = (Exchange) data;
		return Double.toString(e.amount);
	}

	static List<Exchange> read(String text, boolean forInputs) {
		if (text == null)
			return Collections.emptyList();
		String[] lines = text.toString().split("\n");
		List<Exchange> list = new ArrayList<>();
		Mapper mapper = new Mapper();
		for (String line : lines) {
			String[] row = line.split("\t");
			for (int k = 0; k < row.length; k++) {
				row[k] = row[k].trim();
			}
			if (row.length > 2 && Strings.nullOrEqual(row[2], M.Amount))
				continue; // the header row
			Exchange e = mapper.doIt(row, forInputs);
			if (e != null) {
				list.add(e);
			}
		}
		return list;
	}

	private static class Mapper {
		final Logger log = LoggerFactory.getLogger(getClass());
		final FlowDao flowDao = new FlowDao(Database.get());

		Exchange doIt(String[] row, boolean isInput) {
			if (row == null || row.length == 0)
				return null;
			log.trace("create exchange '{}' from clipboard", row[0]);
			Flow flow = findFlow(row);
			if (flow == null)
				return null;
			Exchange e = new Exchange();
			e.flow = flow;
			e.isInput = isInput;
			mapAmount(e, row);
			mapUnit(e, row);
			if (row.length > 5)
				e.uncertainty = Uncertainty.fromString(row[5]);
			if (row.length > 9)
				e.description = row[9];
			// TODO: costs/revenues; avoided tech. flows; data quality
			return e;
		}

		Flow findFlow(String[] row) {
			String fullName = row[0];
			if (fullName == null)
				return null;
			List<Flow> candidates = new ArrayList<>();
			candidates.addAll(flowDao.getForName(fullName));

			// the full name may contains a location code
			String name = null;
			String locationCode = null;
			if (fullName.contains(" - ")) {
				int splitIdx = fullName.lastIndexOf(" - ");
				name = fullName.substring(0, splitIdx).trim();
				locationCode = fullName.substring(splitIdx + 3).trim();
				candidates.addAll(flowDao.getForName(name));
			}

			if (candidates.isEmpty()) {
				log.warn("Could not find flow '{}' in database", fullName);
				return null;
			}
			Flow selected = null;
			for (Flow candidate : candidates) {
				if (selected == null) {
					selected = candidate;
					continue;
				}
				if (matchCategory(candidate, row)
						&& !matchCategory(selected, row)) {
					selected = candidate;
					continue;
				}
				if (matchLocation(candidate, locationCode)
						&& !matchLocation(selected, locationCode)) {
					selected = candidate;
					continue;
				}
			}
			return selected;
		}

		private boolean matchCategory(Flow flow, String[] row) {
			if (flow == null)
				return false;
			if (row.length < 2)
				return flow.getCategory() == null;
			String path = CategoryPath.getShort(flow.getCategory());
			return Strings.nullOrEqual(path, row[1]);
		}

		private boolean matchLocation(Flow flow, String code) {
			if (flow == null)
				return false;
			if (flow.getLocation() == null)
				return code == null;
			if (code == null)
				return flow.getLocation() == null;
			return Strings.nullOrEqual(flow.getLocation().getCode(), code);
		}

		private void mapAmount(Exchange e, String[] row) {
			if (row.length < 3) {
				e.amount = 1.0;
				return;
			}
			try {
				e.amount = Double.parseDouble(row[2]);
			} catch (Exception ex) {
				log.warn("The amount is not numeric; set it to 1.0");
				e.amount = 1.0;
			}
		}

		private void mapUnit(Exchange e, String[] row) {
			if (row.length < 4) {
				setRefUnit(e);
				return;
			}
			if (e.flow == null)
				return;
			String unit = row[3];
			if (mapUnit(e, e.flow.getReferenceFactor(), unit))
				return;
			for (FlowPropertyFactor f : e.flow.getFlowPropertyFactors()) {
				if (mapUnit(e, f, unit))
					return;
			}
			setRefUnit(e);
		}

		private void setRefUnit(Exchange e) {
			if (e == null || e.flow == null)
				return;
			FlowPropertyFactor refFactor = e.flow.getReferenceFactor();
			e.flowPropertyFactor = refFactor;
			if (refFactor == null || refFactor.getFlowProperty() == null)
				return;
			UnitGroup ug = refFactor.getFlowProperty().getUnitGroup();
			if (ug == null)
				return;
			e.unit = ug.getReferenceUnit();
		}

		private boolean mapUnit(Exchange e, FlowPropertyFactor factor, String unit) {
			if (e == null || factor == null)
				return false;
			FlowProperty fp = factor.getFlowProperty();
			if (fp == null || fp.getUnitGroup() == null)
				return false;
			Unit u = fp.getUnitGroup().getUnit(unit);
			if (u == null)
				return false;
			e.flowPropertyFactor = factor;
			e.unit = u;
			return true;
		}

	}

}