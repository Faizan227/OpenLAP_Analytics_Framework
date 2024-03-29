package com.openlap.Visualizer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;

/**
 * The model representing the Data Transformer (Data Adapters) concrete implementations
 *
 * @author Faizan Riaz
 */
@Entity
public class VisualizationDataTransformerMethod {
    @Id
   // @GeneratedValue(strategy = GenerationType.IDENTITY)
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Type(type = "objectid")
    String id;
    //@Column(nullable = false, unique = true)
    private String implementingClass;
    //@Column(nullable = false)
    private String name;

    public VisualizationDataTransformerMethod() {
    }

    public VisualizationDataTransformerMethod(String name, String implementingClass) {
        this.name = name;
        this.implementingClass = implementingClass;
    }

    public String getId() {

        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getImplementingClass() {
        return implementingClass;
    }

    public void setImplementingClass(String implementingClass) {
        this.implementingClass = implementingClass;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VisualizationDataTransformerMethod that = (VisualizationDataTransformerMethod) o;
        return id.equals(that.id) &&
                implementingClass.equals(that.implementingClass) &&
                name.equals(that.name) ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, implementingClass, name);
    }
}
