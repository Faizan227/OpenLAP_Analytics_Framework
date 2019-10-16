package com.openlap.AnalyticsEngine.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.bson.types.ObjectId;
import org.json.JSONException;

import com.openlap.AnalyticsEngine.dto.QueryParameters;

import de.rwthaachen.openlap.dataset.OpenLAPDataSet;
import de.rwthaachen.openlap.exceptions.OpenLAPDataColumnException;

public interface StatementService {
	OpenLAPDataSet getAllVerbsFromStatements(ObjectId organizationId, ObjectId lrsId)
			throws JSONException, OpenLAPDataColumnException;

	OpenLAPDataSet getAllStatementsByCustomQuery(ObjectId organizationId, ObjectId lrsId,
			QueryParameters queryParameters) throws JSONException, OpenLAPDataColumnException, JsonProcessingException;

}
