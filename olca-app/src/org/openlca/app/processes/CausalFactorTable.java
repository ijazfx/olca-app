package org.openlca.app.processes;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.util.CategoryPath;
import org.openlca.app.util.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.core.model.AllocationFactor;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Process;
import org.openlca.util.Strings;

/**
 * A table for the display and editing of the causal allocation factors of a
 * process. The output products are displayed in columns.
 */
class CausalFactorTable {

	private Process process;
	private Column[] columns;
	private TableViewer viewer;

	public CausalFactorTable(ProcessEditor editor) {
		this.process = editor.getModel();
		initColumns();
	}

	private void initColumns() {
		List<Exchange> products = Processes.getOutputProducts(process);
		columns = new Column[products.size()];
		for (int i = 0; i < columns.length; i++)
			columns[i] = new Column(products.get(i));
		Arrays.sort(columns);
	}

	public void render(Section section, FormToolkit toolkit) {
		Composite composite = UI.sectionClient(section, toolkit);
		UI.gridLayout(composite, 1);
		viewer = Tables.createViewer(composite, getColumnTitles());
		viewer.setLabelProvider(new FactorLabel());
		Tables.bindColumnWidths(viewer, 0.2, 0.1, 0.1, 0.1);
		viewer.setInput(Processes.getNonOutputProducts(process));
	}

	private String[] getColumnTitles() {
		String[] titles = new String[columns.length + 4];
		titles[0] = Messages.Flow;
		titles[1] = Messages.Direction;
		titles[2] = Messages.Category;
		titles[3] = Messages.Amount;
		for (int i = 0; i < columns.length; i++)
			titles[i + 4] = columns[i].getTitle();
		return titles;
	}

	private AllocationFactor getFactor(Exchange product, Exchange exchange) {
		AllocationFactor factor = null;
		for (AllocationFactor f : process.getAllocationFactors()) {
			if (f.getAllocationType() != AllocationMethod.CAUSAL)
				continue;
			if (product.getFlow().getId() == f.getProductId()
					&& Objects.equals(f.getExchange(), exchange)) {
				factor = f;
				break;
			}
		}
		return factor;
	}

	private class FactorLabel extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int col) {
			if (col != 0)
				return null;
			if (!(element instanceof Exchange))
				return null;
			Exchange exchange = (Exchange) element;
			if (exchange.getFlow() == null)
				return null;
			return Images.getIcon(exchange.getFlow().getFlowType());
		}

		@Override
		public String getColumnText(Object element, int col) {
			if (!(element instanceof Exchange))
				return null;
			Exchange exchange = (Exchange) element;
			if (exchange.getFlow() == null || exchange.getUnit() == null)
				return null;
			switch (col) {
			case 0:
				return Labels.getDisplayName(exchange.getFlow());
			case 1:
				return exchange.isInput() ? "Input" : "Output";
			case 2:
				return CategoryPath.getShort(exchange.getFlow().getCategory());
			case 3:
				return Numbers.format(exchange.getAmountValue()) + " "
						+ exchange.getUnit().getName();
			default:
				return getFactorLabel(exchange, col);
			}
		}

		private String getFactorLabel(Exchange exchange, int col) {
			int idx = col - 4;
			if (idx < 0 || idx > (columns.length - 1))
				return null;
			Column column = columns[idx];
			AllocationFactor factor = getFactor(column.getProduct(), exchange);
			if (factor == null)
				return Numbers.format(1.0, 2);
			else
				return Double.toString(factor.getValue());
		}
	}

	private class Column implements Comparable<Column> {

		private Exchange product;
		private String key;

		public Column(Exchange product) {
			this.product = product;
			key = UUID.randomUUID().toString();
		}

		public Exchange getProduct() {
			return product;
		}

		public String getKey() {
			return key;
		}

		public String getTitle() {
			if (product == null || product.getFlow() == null)
				return "";
			return Labels.getDisplayName(product.getFlow());
		}

		@Override
		public int compareTo(Column o) {
			return Strings.compare(this.getTitle(), o.getTitle());
		}

	}

}
