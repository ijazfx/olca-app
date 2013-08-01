package org.openlca.app.wizards;

import java.util.UUID;

import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessDocumentation;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;

/**
 * Controller for the creation of a process from wizard data. The user can
 * decide to create a new product with the process (Data#createWithProduct =
 * true) or to set a flow as a reference flow for the process. For the first
 * case, a flow property must be provided for the second case a flow. If a flow
 * is created the flow is also inserted in the database.
 */
class ProcessCreationController {

	private IDatabase database;
	private String name;
	private String description;
	private boolean createWithProduct;
	private BaseDescriptor flowProperty;
	private FlowDescriptor flow;

	public ProcessCreationController(IDatabase database) {
		this.database = database;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setCreateWithProduct(boolean createWithProduct) {
		this.createWithProduct = createWithProduct;
	}

	public void setFlowProperty(BaseDescriptor flowProperty) {
		this.flowProperty = flowProperty;
	}

	public void setFlow(FlowDescriptor flow) {
		this.flow = flow;
	}

	public boolean canCreate() {
		if (name == null || name.trim().isEmpty())
			return false;
		if (createWithProduct)
			return flowProperty != null;
		return flow != null;
	}

	public Process create() {
		if (!canCreate())
			throw new RuntimeException("Invalid arguments for process creation");
		try {
			Process process = new Process();
			process.setRefId(UUID.randomUUID().toString());
			process.setName(name);
			process.setDescription(description);
			Flow flow = getFlow();
			addQuantitativeReference(process, flow);
			ProcessDocumentation doc = new ProcessDocumentation();
			doc.setId(process.getId());
			process.setDocumentation(doc);
			return process;
		} catch (Exception e) {
			throw new RuntimeException("Could not create process", e);
		}
	}

	private Flow getFlow() throws Exception {
		if (createWithProduct)
			return createFlow();
		return new FlowDao(database).getForId(flow.getId());
	}

	private Flow createFlow() throws Exception {
		Flow flow;
		flow = new Flow();
		flow.setRefId(UUID.randomUUID().toString());
		flow.setName(name);
		flow.setDescription(description);
		flow.setFlowType(FlowType.PRODUCT_FLOW);
		FlowProperty property = database.createDao(FlowProperty.class)
				.getForId(flowProperty.getId());
		flow.setReferenceFlowProperty(property);
		FlowPropertyFactor factor = new FlowPropertyFactor();
		factor.setConversionFactor(1);
		factor.setFlowProperty(property);
		flow.getFlowPropertyFactors().add(factor);
		database.createDao(Flow.class).insert(flow);
		return flow;
	}

	private void addQuantitativeReference(Process process, Flow flow) {
		Exchange qRef = new Exchange();
		qRef.setFlow(flow);
		FlowProperty refProp = flow.getReferenceFlowProperty();
		qRef.setFlowPropertyFactor(flow.getReferenceFactor());
		UnitGroup unitGroup = refProp.getUnitGroup();
		if (unitGroup != null)
			qRef.setUnit(unitGroup.getReferenceUnit());
		qRef.setInput(false);
		process.getExchanges().add(qRef);
		process.setQuantitativeReference(qRef);
	}
}