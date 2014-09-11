package com.hp.score.samples.openstack;

import com.google.common.collect.Sets;
import com.hp.score.api.ExecutionPlan;
import com.hp.score.api.TriggeringProperties;
import com.hp.score.samples.openstack.actions.ExecutionPlanBuilder;
import com.hp.score.samples.openstack.actions.InputBinding;
import com.hp.score.samples.openstack.actions.InputBindingFactory;
import com.hp.score.samples.openstack.actions.MatchType;
import com.hp.score.samples.openstack.actions.NavigationMatcher;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hp.score.samples.openstack.OpenstackCommons.OPENSTACK_UTILS_CLASS;
import static com.hp.score.samples.openstack.OpenstackCommons.GET_MULTI_INSTANCE_RESPONSE_METHOD;
import static com.hp.score.samples.openstack.OpenstackCommons.SERVER_NAMES_LIST_MESSAGE;
import static com.hp.score.samples.openstack.OpenstackCommons.SERVER_NAMES_LIST_KEY;
import static com.hp.score.samples.openstack.OpenstackCommons.SERVER_NAME_KEY;
import static com.hp.score.samples.openstack.OpenstackCommons.SERVER_NAME_MESSAGE;

import static com.hp.score.samples.openstack.OpenstackCommons.SPLIT_SERVERS_INTO_BRANCH_CONTEXTS_METHOD;
import static com.hp.score.samples.openstack.OpenstackCommons.createFailureStep;
import static com.hp.score.samples.openstack.OpenstackCommons.createSuccessStep;
import static com.hp.score.samples.openstack.OpenstackCommons.mergeInputsWithoutDuplicates;
import static com.hp.score.samples.openstack.OpenstackCommons.SUCCESS_RESPONSE;
import static com.hp.score.samples.openstack.OpenstackCommons.RESPONSE_KEY;


/**
 * Date: 8/29/2014
 *
 * @author lesant
 */
@SuppressWarnings("unused")
public class CreateMultiInstanceServersFlow {
	private List<InputBinding> inputBindings;

	@SuppressWarnings("unused")
	public CreateMultiInstanceServersFlow() {
		inputBindings = generateInitialInputBindings();
	}
	@SuppressWarnings("unused")
	public List<InputBinding> getInputBindings() {
		return inputBindings;
	}

	private List<InputBinding> generateInitialInputBindings() {
		@SuppressWarnings("unchecked") List<InputBinding> bindings = mergeInputsWithoutDuplicates(
				new CreateServerFlow().getInputBindings());

		bindings.remove(InputBindingFactory.createInputBinding(SERVER_NAME_MESSAGE, SERVER_NAME_KEY, true));
		bindings.add(InputBindingFactory.createInputBinding(SERVER_NAMES_LIST_MESSAGE, SERVER_NAMES_LIST_KEY, true));


		return bindings;
	}

	@SuppressWarnings("unused")
	public TriggeringProperties createMultiInstanceServersFlow(){
		ExecutionPlanBuilder builder = new ExecutionPlanBuilder();

		Long singleStepId = 0L;
		Long createServerJoinId = 1L;
		Long createServerSplitId = 2L;
		Long getMultiInstanceResponseStepId = 3L;
		Long successStepId = 4L;
		Long failureStepId = 5L;

		CreateServerFlow createServer = new CreateServerFlow();
		ExecutionPlan createServerExecutionPlan = createServer.createServerFlow().getExecutionPlan();
		String createServerFlowUuid = createServerExecutionPlan.getFlowUuid();

		List<NavigationMatcher<Serializable>> navigationMatchers = new ArrayList<>();

		navigationMatchers.add(new NavigationMatcher<Serializable>(MatchType.DEFAULT, getMultiInstanceResponseStepId));
		builder.addStep(singleStepId, OPENSTACK_UTILS_CLASS, SPLIT_SERVERS_INTO_BRANCH_CONTEXTS_METHOD, createServerSplitId);
		builder.addMultiInstance(createServerSplitId, createServerJoinId, createServerFlowUuid, navigationMatchers);

		navigationMatchers = new ArrayList<>();
		navigationMatchers.add(new NavigationMatcher<Serializable>(MatchType.EQUAL, RESPONSE_KEY, SUCCESS_RESPONSE, successStepId));
		navigationMatchers.add(new NavigationMatcher<Serializable>(MatchType.DEFAULT, failureStepId));

		builder.addOOActionStep(getMultiInstanceResponseStepId, OPENSTACK_UTILS_CLASS, GET_MULTI_INSTANCE_RESPONSE_METHOD, null, navigationMatchers);
		createSuccessStep(builder, successStepId);
		createFailureStep(builder, failureStepId);

		ExecutionPlan parallelFlow = builder.getExecutionPlan();
		parallelFlow.setSubflowsUUIDs(Sets.newHashSet(createServerFlowUuid));
		Map<String, ExecutionPlan> dependencies = new HashMap<>();
		dependencies.put(createServerFlowUuid, createServerExecutionPlan);
		Map<String, Serializable> getRuntimeValues = new HashMap<>();

		return TriggeringProperties.create(parallelFlow).
				setDependencies(dependencies).setRuntimeValues(getRuntimeValues).setStartStep(0L);
	}




}