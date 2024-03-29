package com.openlap.AnalyticsEngine.service;

import java.io.IOException;
import java.util.ArrayList;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.openlap.AnalyticsEngine.dto.OpenLapDataConverter;
import com.openlap.AnalyticsEngine.model.Personas;
import com.openlap.AnalyticsEngine.repo.PersonasRepo;

import de.rwthaachen.openlap.dataset.OpenLAPColumnDataType;
import de.rwthaachen.openlap.dataset.OpenLAPDataSet;
import de.rwthaachen.openlap.exceptions.OpenLAPDataColumnException;

@Service
public class PersonasServiceImp implements PersonasService {
	@Autowired
	private PersonasRepo personasRepo;

	@Override
	public OpenLAPDataSet listOfPersonNamesByOrganization(ObjectId OrganizationId)
			throws IOException, OpenLAPDataColumnException {
		ArrayList listOfPerson = new ArrayList();
		ArrayList listOfPersonIds = new ArrayList();
		// ObjectId id = new ObjectId(OrganizationId);

		for (Personas person : personasRepo.findPersonNamesByOrganization(OrganizationId)) {

			listOfPersonIds.add(person.getId());
			listOfPerson.add(person.getName());
		}

		OpenLapDataConverter dataConveter = new OpenLapDataConverter();
		dataConveter.SetOpenLapDataColumn("PersonIds", OpenLAPColumnDataType.Text, true, listOfPersonIds);
		dataConveter.SetOpenLapDataColumn("PersonNames", OpenLAPColumnDataType.Text, true, listOfPerson);

		return dataConveter.getDataSet();
	}

}
