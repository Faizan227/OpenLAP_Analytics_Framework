package com.openlap.AnalyticsEngine.service;

import java.io.IOException;

import org.springframework.security.core.Authentication;

import de.rwthaachen.openlap.dataset.OpenLAPDataSet;
import de.rwthaachen.openlap.exceptions.OpenLAPDataColumnException;

public interface OrganizationService {
	OpenLAPDataSet getOrganizationForLoggedUser(Authentication authentication)
			throws IOException, OpenLAPDataColumnException;
}
