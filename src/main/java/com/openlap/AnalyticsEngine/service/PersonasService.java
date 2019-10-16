package com.openlap.AnalyticsEngine.service;

import java.io.IOException;

import org.bson.types.ObjectId;

import de.rwthaachen.openlap.dataset.OpenLAPDataSet;
import de.rwthaachen.openlap.exceptions.OpenLAPDataColumnException;

public interface PersonasService {
	OpenLAPDataSet listOfPersonNamesByOrganization(ObjectId OrganizationId)
			throws IOException, OpenLAPDataColumnException;
}
