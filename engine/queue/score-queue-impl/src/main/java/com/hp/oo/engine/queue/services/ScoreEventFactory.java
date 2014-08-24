package com.hp.oo.engine.queue.services;

import com.hp.oo.internal.sdk.execution.Execution;
import com.hp.score.events.ScoreEvent;

/**
 * User: maromg
 * Date: 30/07/2014
 */
public interface ScoreEventFactory {

	public ScoreEvent createFinishedEvent(Execution execution);

	public ScoreEvent createFailedBranchEvent(Execution execution);

	public ScoreEvent createFailureEvent(Execution execution);

	public ScoreEvent createNoWorkerEvent(Execution execution, Long pauseId);

}