package com.openlap.AnalyticsEngine.dto;

import java.util.ArrayList;

import de.rwthaachen.openlap.dataset.OpenLAPColumnDataType;
import de.rwthaachen.openlap.dataset.OpenLAPDataColumn;
import de.rwthaachen.openlap.dataset.OpenLAPDataSet;
import de.rwthaachen.openlap.exceptions.OpenLAPDataColumnException;

public class OpenLapDataConverter {

	OpenLAPDataColumn column;
	OpenLAPDataSet dataSet = new OpenLAPDataSet();

	/*
	 * public OpenLapDataConverter(String ColumnId, OpenLAPColumnDataType type,
	 * Boolean required, ArrayList data) throws OpenLAPDataColumnException {
	 * this.column = new OpenLAPDataColumn(ColumnId, type, required);
	 * this.column.setData(data); this.dataSet.addOpenLAPDataColumn(this.column); }
	 */

	public void SetOpenLapDataColumn(String ColumnId, OpenLAPColumnDataType type, Boolean required, ArrayList data)
			throws OpenLAPDataColumnException {
		this.column = new OpenLAPDataColumn(ColumnId, type, required);
		this.column.setData(data);
		this.dataSet.addOpenLAPDataColumn(this.column);

	}

	public void SetOpenLapDataColumn(String ColumnId, OpenLAPColumnDataType type, Boolean required, ArrayList data, String title, String desc)
			throws OpenLAPDataColumnException {
		this.column = new OpenLAPDataColumn(ColumnId, type, required, title, desc);
		this.column.setData(data);
		this.dataSet.addOpenLAPDataColumn(this.column);

	}

	public OpenLAPDataSet getDataSet() {
		return dataSet;
	}

}
