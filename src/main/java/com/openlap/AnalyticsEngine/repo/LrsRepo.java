package com.openlap.AnalyticsEngine.repo;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.openlap.AnalyticsEngine.model.Lrs;



public interface LrsRepo extends MongoRepository<Lrs, String> {
	@Query(value = "{'organisation':?0 }", fields = "{'_id':1,'title':1}")
	List<Lrs> findLrsByOrganizationsId(ObjectId organizationId);
}
