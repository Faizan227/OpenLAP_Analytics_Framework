package com.openlap.Visualizer.dtos.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.openlap.Visualizer.model.VisualizationLibrary;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UploadVisualizationLibraryRequest {

    private List<VisualizationLibrary> visualizationLibraries;

    public List<VisualizationLibrary> getVisualizationLibraries() {
        return visualizationLibraries;
    }

    public void setVisualizationLibraries(List<VisualizationLibrary> visualizationLibraries) {
        this.visualizationLibraries = visualizationLibraries;
    }
}
