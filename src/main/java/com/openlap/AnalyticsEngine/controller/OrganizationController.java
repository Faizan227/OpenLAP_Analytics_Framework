package com.openlap.AnalyticsEngine.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.openlap.AnalyticsEngine.service.OrganizationService;

import de.rwthaachen.openlap.dataset.OpenLAPDataSet;
import de.rwthaachen.openlap.exceptions.OpenLAPDataColumnException;

@RestController
@RequestMapping("/v1/organizations/")
public class OrganizationController {
	@Autowired
	private OrganizationService organizationService;

	/**
	 * 
	 * @param authentication
	 * @return List of organization for logged user
	 * @throws IOException
	 * @throws OpenLAPDataColumnException
	 */
	@PreAuthorize("hasRole('site_admin')")
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	@ResponseBody
	public OpenLAPDataSet list(Authentication authentication) throws IOException, OpenLAPDataColumnException {
		return organizationService.getOrganizationForLoggedUser(authentication);

	}
}
