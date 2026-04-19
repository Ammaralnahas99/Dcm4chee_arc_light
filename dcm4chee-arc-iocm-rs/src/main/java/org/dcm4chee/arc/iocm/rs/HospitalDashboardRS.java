package org.dcm4chee.arc.iocm.rs;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.stream.JsonGenerator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.dcm4chee.arc.conf.ArchiveDeviceExtension;
import org.dcm4chee.arc.keycloak.HttpServletRequestInfo;
import org.dcm4chee.arc.keycloak.KeycloakContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

/**
 * @author Hospital Dashboard REST Service
 */
@RequestScoped
@Path("aets/{AETitle}/rs")
public class HospitalDashboardRS {

    private static final Logger LOG = LoggerFactory.getLogger(HospitalDashboardRS.class);

    @PersistenceContext(unitName = "dcm4chee-arc")
    private EntityManager em;

    @PathParam("AETitle")
    private String aet;

    @GET
    @Path("/hospitals/list")
    @Produces("application/json")
    public Response getHospitalList() {
        LOG.info("Getting list of hospitals");
        
        try {
            // Query to get distinct hospital names
            String sql = "SELECT DISTINCT p.hospital_name " +
                    "FROM patient p " +
                    "WHERE p.hospital_name IS NOT NULL " +
                    "ORDER BY p.hospital_name";

            List<String> hospitalNames = em.createNativeQuery(sql).getResultList();

            StreamingOutput output = out -> {
                try (JsonGenerator gen = Json.createGenerator(out)) {
                    gen.writeStartArray();
                    for (String hospitalName : hospitalNames) {
                        gen.write(hospitalName);
                    }
                    gen.writeEnd();
                }
            };

            return Response.ok(output).build();
            
        } catch (Exception e) {
            LOG.error("Error getting hospital list", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/hospitals/statistics")
    @Produces("application/json")
    public Response getHospitalStatistics() {
        LOG.info("Getting hospital statistics");
        
        try {
            // Query to get patient and study counts grouped by hospital_name
            String sql = "SELECT " +
                    "p.hospital_name, " +
                    "COUNT(DISTINCT p.pk) as patient_count, " +
                    "COUNT(DISTINCT s.pk) as study_count " +
                    "FROM patient p " +
                    "LEFT JOIN study s ON s.patient_fk = p.pk " +
                    "WHERE p.hospital_name IS NOT NULL " +
                    "GROUP BY p.hospital_name " +
                    "ORDER BY p.hospital_name";

            List<Object[]> results = em.createNativeQuery(sql).getResultList();

            StreamingOutput output = out -> {
                try (JsonGenerator gen = Json.createGenerator(out)) {
                    gen.writeStartArray();
                    
                    for (Object[] row : results) {
                        String hospitalName = (String) row[0];
                        Long patientCount = ((Number) row[1]).longValue();
                        Long studyCount = ((Number) row[2]).longValue();
                        
                        gen.writeStartObject();
                        gen.write("name", hospitalName != null ? hospitalName : "Unknown");
                        gen.write("patients", patientCount);
                        gen.write("studies", studyCount);
                        gen.write("active", true);
                        gen.writeEnd();
                    }
                    
                    gen.writeEnd();
                }
            };

            return Response.ok(output).build();
            
        } catch (Exception e) {
            LOG.error("Error getting hospital statistics", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/hospitals/{hospitalName}/statistics")
    @Produces("application/json")
    public Response getSpecificHospitalStatistics(@PathParam("hospitalName") String hospitalName) {
        LOG.info("Getting statistics for hospital: {}", hospitalName);
        
        try {
            // Query to get patient and study counts for a specific hospital
            String sql = "SELECT " +
                    "p.hospital_name, " +
                    "COUNT(DISTINCT p.pk) as patient_count, " +
                    "COUNT(DISTINCT s.pk) as study_count " +
                    "FROM patient p " +
                    "LEFT JOIN study s ON s.patient_fk = p.pk " +
                    "WHERE p.hospital_name = ?1 " +
                    "GROUP BY p.hospital_name";

            List<Object[]> results = em.createNativeQuery(sql)
                    .setParameter(1, hospitalName)
                    .getResultList();

            StreamingOutput output = out -> {
                try (JsonGenerator gen = Json.createGenerator(out)) {
                    gen.writeStartObject();
                    
                    if (!results.isEmpty()) {
                        Object[] row = results.get(0);
                        String name = (String) row[0];
                        Long patientCount = ((Number) row[1]).longValue();
                        Long studyCount = ((Number) row[2]).longValue();
                        
                        gen.write("name", name != null ? name : "Unknown");
                        gen.write("patients", patientCount);
                        gen.write("studies", studyCount);
                        gen.write("active", true);
                    } else {
                        gen.write("name", hospitalName);
                        gen.write("patients", 0);
                        gen.write("studies", 0);
                        gen.write("active", false);
                    }
                    
                    gen.writeEnd();
                }
            };

            return Response.ok(output).build();
            
        } catch (Exception e) {
            LOG.error("Error getting hospital statistics for " + hospitalName, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/hospitals/{hospitalName}/modalities")
    @Produces("application/json")
    public Response getHospitalModalities(@PathParam("hospitalName") String hospitalName) {
        LOG.info("Getting modalities for hospital: {}", hospitalName);
        
        try {
            // Query to get distinct modalities for a hospital
            String sql = "SELECT DISTINCT s.modality " +
                    "FROM series s " +
                    "JOIN study st ON s.study_fk = st.pk " +
                    "JOIN patient p ON st.patient_fk = p.pk " +
                    "WHERE p.hospital_name = ?1 " +
                    "AND s.modality IS NOT NULL " +
                    "ORDER BY s.modality";

            List<String> modalities = em.createNativeQuery(sql)
                    .setParameter(1, hospitalName)
                    .getResultList();

            StreamingOutput output = out -> {
                try (JsonGenerator gen = Json.createGenerator(out)) {
                    gen.writeStartArray();
                    for (String modality : modalities) {
                        gen.write(modality);
                    }
                    gen.writeEnd();
                }
            };

            return Response.ok(output).build();
            
        } catch (Exception e) {
            LOG.error("Error getting hospital modalities", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }
}
