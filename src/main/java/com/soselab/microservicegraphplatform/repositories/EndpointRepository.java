package com.soselab.microservicegraphplatform.repositories;

import com.soselab.microservicegraphplatform.bean.neo4j.Endpoint;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EndpointRepository extends GraphRepository<Endpoint> {

    Endpoint findByEndpointId(String endpointId);

    @Query("MATCH (e:Endpoint)<-[:OWN]-(m:Service) WHERE m.appId = {appId} AND e.endpointId = {endpointId} RETURN e")
    Endpoint findByEndpointIdAndAppId(@Param("endpointId") String endpointId, @Param("appId") String appId);

    @Query("MATCH (e:NullEndpoint)<-[:OWN]-(m:Service) WHERE m.appId = {appId} AND e.endpointId = {endpointId} RETURN e")
    Endpoint findByNullEndpointAndAppId(@Param("endpointId") String endpointId, @Param("appId") String appId);

    @Query("MATCH (sm:Service)-[:REGISTER]->(:ServiceRegistry)<-[:REGISTER]-(tm:Service)-[:OWN]->(te:Endpoint) " +
            "WHERE sm.appId = {smId} AND tm.appName = {tmName} AND tm.version = {tmVer} AND te.endpointId = {teId} RETURN te")
    Endpoint findTargetEndpoint(@Param("smId") String sourceAppId, @Param("tmName") String targetAppName,
                                @Param("tmVer") String targetVersion, @Param("teId") String targetEndpointId);

    @Query("MATCH (:Service {appId:{appId}})-[:OWN]->(e:Endpoint) RETURN e")
    List<Endpoint> findByAppId(@Param("appId") String appId);

    @Query("MATCH (sm:Service)-[:REGISTER]->(:ServiceRegistry)<-[:REGISTER]-(tm:Service)-[:OWN]->(te:Endpoint) " +
            "WHERE sm.appId = {smId} AND tm.appName = {tmName} AND te.endpointId = {teId} RETURN te")
    List<Endpoint> findTargetEndpointNotSpecVer(@Param("smId") String sourceAppId, @Param("tmName") String targetAppName,
                                               @Param("teId") String targetEndpointId);

    @Query("MATCH (ne:NullEndpoint) WHERE NOT (ne)<-[:HTTP_REQUEST]-() DETACH DELETE ne")
    void deleteUselessNullEndpoint();

    @Query("MATCH (:Service {appId:{appId}})-[:OWN]->(e:NullEndpoint {endpointId:{endpointId}}) REMOVE e:NullEndpoint")
    void removeNullLabelByAppIdAAndEndpointId(@Param("appId") String appId, @Param("endpointId") String endpointId);

}