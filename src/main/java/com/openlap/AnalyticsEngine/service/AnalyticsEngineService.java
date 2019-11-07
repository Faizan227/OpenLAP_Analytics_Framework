package com.openlap.AnalyticsEngine.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.openlap.AnalyticsEngine.Exceptions.BadRequestException;
import com.openlap.AnalyticsEngine.Exceptions.ItemNotFoundException;
import com.openlap.AnalyticsEngine.dto.QueryParameters;
import com.openlap.AnalyticsEngine.dto.Request.IndicatorPreviewRequest;
import com.openlap.AnalyticsEngine.dto.Request.IndicatorSaveRequest;
import com.openlap.AnalyticsEngine.dto.Request.QuestionSaveRequest;
import com.openlap.AnalyticsEngine.dto.Response.*;
import com.openlap.AnalyticsEngine.dto.WrapperRequest;
import com.openlap.AnalyticsEngine.model.*;
import com.openlap.AnalyticsMethods.services.AnalyticsMethodsClassPathLoader;
import com.openlap.AnalyticsModules.model.OpenLAPDataSetMergeMapping;
import com.openlap.AnalyticsMethods.model.AnalyticsMethods;
import com.openlap.AnalyticsMethods.services.AnalyticsMethodsService;
import com.openlap.AnalyticsModules.model.*;
import com.openlap.OpenLAPAnalyaticsFramework;
import com.openlap.Visualizer.dtos.request.GenerateVisualizationCodeRequest;
import com.openlap.Visualizer.dtos.request.ValidateVisualizationTypeConfigurationRequest;
import com.openlap.Visualizer.dtos.response.*;
import com.openlap.Visualizer.model.VisualizationType;
import core.AnalyticsMethod;
import core.exceptions.AnalyticsMethodInitializationException;
import de.rwthaachen.openlap.analyticsmodules.model.AnalyticsMethodEntry;
import de.rwthaachen.openlap.dataset.*;
import de.rwthaachen.openlap.dynamicparam.OpenLAPDynamicParam;
import de.rwthaachen.openlap.exceptions.OpenLAPDataColumnException;
import de.rwthaachen.openlap.visualizer.core.dtos.response.ValidateVisualizationMethodConfigurationResponse;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import protostream.com.google.gson.Gson;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.TransactionManager;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.util.*;

@Service
public class AnalyticsEngineService {

    private static final Logger log = LoggerFactory.getLogger(OpenLAPAnalyaticsFramework.class);

    Gson gson = new Gson();
    @Value("${indicatorExecutionURL}")
    private String indicatorExecutionURL;

    private ObjectId organizationId = new ObjectId("5d669b0f06edcb1328d5e8b9");

    private ObjectId lrsId = new ObjectId("5d67c3081a73ca31d4d0d425");

    @Autowired
    public StatementServiceImp statementServiceImp;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    private static final Logger logger = LoggerFactory.getLogger(OpenLapUser.class);

    WrapperRequest wrapperRequest;

    private long executionCount = 0;
    private long previewCount = 0;

    TransactionManager tm = com.arjuna.ats.jta.TransactionManager.transactionManager();
    EntityManagerFactory factory = Persistence.createEntityManagerFactory("OpenLAP");
    EntityManager em = factory.createEntityManager();

    @Autowired
    private AnalyticsMethodsService analyticsMethodsService;

    //region Questions
    public Question getQuestionById(String id){
        Question result =em.find(Question.class, id);
        if (result == null || id == null) {
            throw new ItemNotFoundException("Question with id: {" + id + "} not found", "2");
        } else {
            return result;
        }
    }

    public Question saveQuestion(Question analyticsEngineQuestion) {

        //Question questionToSave = new Question(question.getName(), question.getIndicatorCount(), question.getGoal(), question.getIndicators());
        try {
            tm.begin();
            em.persist(analyticsEngineQuestion);
            //Commit
            em.flush();
            em.close();
            tm.commit();
            factory.close();
            return analyticsEngineQuestion;
        } catch (DataIntegrityViolationException mongoException) {
            mongoException.printStackTrace();
            throw new BadRequestException("Question already exists.", "1");
        } catch (Exception e) {
            e.printStackTrace();
            throw new BadRequestException(e.getMessage(), "1");
        }
    }

    public List<QuestionResponse> getQuestions(HttpServletRequest request) {
        List<QuestionResponse> questionsResponse = new ArrayList<QuestionResponse>();

        List<Question> allQuestions = getAllQuestions();

        for (Question question : allQuestions) {
            questionsResponse.add(new QuestionResponse(question.getId(), question.getName(), question.getIndicatorCount()));
        }

        return questionsResponse;
    }

    public List<Question> getAllQuestions() {
        String query = "From Question";
        List<Question> questions = em.createQuery(query).getResultList();
        return questions;
    }

    public Boolean isQuestionNameAvailable(String name) throws ItemNotFoundException {

        String query = "From Question a where name = "+name+" ";
        List<Question> result = em.createQuery(query).getResultList();

        if (result == null) {
            return true;
        } else {
            if (result.size() > 0)
                return false;
            else
                return true;
        }
    }

  /*  public List<Question> searchQuestions(String searchParameter, boolean exactSearch, String colName, String sortDirection, boolean sort) {
        ArrayList<Question> result = new ArrayList<Question>();

        Sort.Direction querySortDirection;
        switch(sortDirection.toLowerCase()){
            case "asc":
                querySortDirection = Sort.Direction.ASC;
                break;
            case "desc":
                querySortDirection = Sort.Direction.DESC;
                break;
            default:
                querySortDirection = Sort.Direction.ASC;
        }

        if(exactSearch) {
            if(sort)

                questionRepository.findByName(searchParameter, new Sort(querySortDirection, colName)).forEach(result::add);
            else
                questionRepository.findByName(searchParameter).forEach(result::add);
        }
        else {
            if(sort)
                questionRepository.findByNameContaining(searchParameter, new Sort(querySortDirection, colName)).forEach(result::add);
            else
                questionRepository.findByNameContaining(searchParameter).forEach(result::add);
        }
        return result;
    }

    public List<Question> getSortedQuestions(String colName, String sortDirection, boolean sort) {
        ArrayList<Question> result = new ArrayList<Question>();

        Sort.Direction querySortDirection;
        switch(sortDirection.toLowerCase()){
            case "asc":
                querySortDirection = Sort.Direction.ASC;
                break;
            case "desc":
                querySortDirection = Sort.Direction.DESC;
                break;
            default:
                querySortDirection = Sort.Direction.ASC;
        }

        if(sort)
            questionRepository.findAll(new Sort(querySortDirection, colName)).forEach(result::add);
        else
            questionRepository.findAll().forEach(result::add);

        return result;
    }*/

    public void deleteQuestion(String id) {

        Question result = em.find(Question.class, id);

        if (result==null || id == null) {
            throw new ItemNotFoundException("Question with id = {" + id + "} not found.", "2");
        }else {
            em.getTransaction().begin();
            em.remove(result);
            em.getTransaction().commit();
        }
    }

    public Boolean validateQuestionName(String name) {
        return isQuestionNameAvailable(name);
    }

    //endregion

    public List<IndicatorResponse> getIndicators(HttpServletRequest request) {
        String baseUrl = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());
        ObjectMapper mapper = new ObjectMapper();

        List<IndicatorResponse> indicatorResponses = new ArrayList<IndicatorResponse>();

        List<Triad> triads = null;
        try {
            String triadsJSON = performGetRequest(baseUrl + "/AnalyticsModules/Triads/");
            triads = mapper.readValue(triadsJSON, mapper.getTypeFactory().constructCollectionType(List.class, Triad.class));
        } catch (Exception exc) {
            throw new ItemNotFoundException("No indicator found", "1");
        }

        for (Triad triad : triads) {
            Indicator indicator = getIndicatorById(triad.getIndicatorReference().getIndicators().get("0").getId());

            IndicatorResponse indicatorResponse = new IndicatorResponse();
            indicatorResponse.setId(triad.getId());
            //indicatorResponse.setQuery(indicator.getQuery());
            indicatorResponse.setIndicatorReference(triad.getIndicatorReference());
            indicatorResponse.setAnalyticsMethodReference(triad.getAnalyticsMethodReference());
            indicatorResponse.setVisualizationReference(triad.getVisualizationReference());

            indicatorResponse.setQueryToMethodConfig(new HashMap<>());
            indicatorResponse.getQueryToMethodConfig().put("0", triad.getIndicatorToAnalyticsMethodMapping().getPortConfigs().get("0"));
            indicatorResponse.setMethodToVisualizationConfig(triad.getAnalyticsMethodToVisualizationMapping());
            indicatorResponse.setName(indicator.getName());
            indicatorResponse.setParameters(triad.getParameters());
            //indicatorResponse.setComposite(indicator.isComposite());
            indicatorResponse.setIndicatorType(triad.getIndicatorReference().getIndicatorType());
            indicatorResponse.setCreatedBy(triad.getCreatedBy());

            indicatorResponses.add(indicatorResponse);
        }
        return indicatorResponses;
    }

    public List<IndicatorResponse> getIndicatorsByQuestionId(String questionId, HttpServletRequest request) {
        List<IndicatorResponse> indicators = new ArrayList<IndicatorResponse>();

        Question question = getQuestionById(questionId);

        for (Triad triad : question.getTriads()) {
            Indicator indicator = getIndicatorById(triad.getIndicatorReference().getIndicators().get("0").getId());

            IndicatorResponse indicatorResponse = new IndicatorResponse();
            indicatorResponse.setId(triad.getId());
            //indicatorResponse.setQuery(indicator.getQuery());
            indicatorResponse.setIndicatorReference(triad.getIndicatorReference());
            //indicatorResponse.setAnalyticsMethodReference(triad.getAnalyticsMethodReference().getAnalyticsMethods().get("0"));
            indicatorResponse.setAnalyticsMethodReference(triad.getAnalyticsMethodReference());
            indicatorResponse.setVisualizationReference(triad.getVisualizationReference());
            //indicatorResponse.setQueryToMethodConfig(triad.getIndicatorToAnalyticsMethodMapping());
            indicatorResponse.setQueryToMethodConfig(new HashMap<>());
            indicatorResponse.getQueryToMethodConfig().put("0",triad.getIndicatorToAnalyticsMethodMapping().getPortConfigs().get("0"));


            indicatorResponse.setMethodToVisualizationConfig(triad.getAnalyticsMethodToVisualizationMapping());
            indicatorResponse.setName(indicator.getName());
            indicatorResponse.setParameters(triad.getParameters());
            //indicatorResponse.setComposite(indicator.isComposite());
            indicatorResponse.setIndicatorType(triad.getIndicatorReference().getIndicatorType());
            indicatorResponse.setCreatedBy(triad.getCreatedBy());

            indicators.add(indicatorResponse);
        }

        return indicators;
    }

    public Indicator getIndicatorById(String id) throws ItemNotFoundException {

        Indicator result = em.find(Indicator.class, id);
        if (result == null || id == null) {
            throw new ItemNotFoundException("Indicator with id: {" + id + "} not found","1");
        } else {
            return result;
        }
    }

    public Boolean isIndicatorNameAvailable(String name) throws ItemNotFoundException {

        String Query = "From Indicator where name = "+name+"";

        List<Indicator> result = em.createQuery(Query).getResultList();
        if (result == null) {
            return true;
        } else {
            if(result.size()>0)
                return false;
            else
                return true;
        }
    }

    public Indicator saveIndicator(Indicator indicator) {
        try {
            em.getTransaction().begin();
            em.persist(indicator);
            //Commit
            em.flush();
            em.close();
            tm.commit();
            factory.close();
            return indicator;
        } catch (DataIntegrityViolationException mongoException) {
            mongoException.printStackTrace();
            throw new BadRequestException("Indicator already exists.", "2");
        } catch (Exception e) {
            e.printStackTrace();
            throw new BadRequestException(e.getMessage(), "2");
        }
    }

    public Boolean validateIndicatorName(String name) {
        return isIndicatorNameAvailable(name);
    }

    //region TriadCache repository methods


    public TriadCache saveTriadCache(String triadId, String code) {

        TriadCache triadCache = new TriadCache(triadId, code);
        try {

            em.getTransaction().begin();
            em.persist(triadCache);
            em.getTransaction().commit();
            em.flush();
            return triadCache;

        } catch (DataIntegrityViolationException sqlException) {
            sqlException.printStackTrace();
            throw new BadRequestException("Cache already exists.", "1");
        } catch (Exception e) {
            e.printStackTrace();
            throw new BadRequestException(e.getMessage(),  "1");
        }
    }

    public TriadCache saveTriadCache(String triadId, String userHash, String code) {

        TriadCache triadCache = new TriadCache(triadId, userHash, code);
        try {

            em.getTransaction().begin();
            em.persist(triadCache);
            em.getTransaction().commit();
            em.flush();
            return triadCache;
        } catch (DataIntegrityViolationException sqlException) {
            sqlException.printStackTrace();
            throw new BadRequestException("Cache already exists.", "1");
        } catch (Exception e) {
            e.printStackTrace();
            throw new BadRequestException(e.getMessage(),  "1");
        }
    }

    //endregion
    //region indicator preview
    public IndicatorPreviewResponse getIndicatorPreview(IndicatorPreviewRequest previewRequest, String baseUrl) {
        long indicatorExecutionStartTime = System.currentTimeMillis();
        long localPreviewCount = previewCount++;
        IndicatorPreviewResponse response = new IndicatorPreviewResponse();
        try {
            ObjectMapper mapper = new ObjectMapper();

                QueryParameters curIndQuery = previewRequest.getQueries().get("0");
                OpenLAPPortConfig queryToMethodConfig = previewRequest.getQueryToMethodConfig().get("0");
/*
                if(curIndQuery !=null) {
                if(!previewRequest.getAdditionalParams().containsKey("rid")){
                    response.setSuccess(false);
                    response.setErrorMessage("This indicator require a user code to generate personalized data which is not provided. Try again after refreshing the web page.");
                    log.error("[Preview-Simple-End],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:rid,code:rid-missing");
                    return response;
                }
                else{
                    String userID = previewRequest.getAdditionalParams().get("rid").toString().toUpperCase();
                    //curIndQuery = curIndQuery.replace("xxxridxxx", userID);
                    }
            }*/

                OpenLAPDataSet queryDataSet = statementServiceImp.getAllStatementsByCustomQuery(organizationId, lrsId, curIndQuery);



                if (queryDataSet == null) {
                    response.setSuccess(false);
                    response.setErrorMessage("No data found for the indicator. Try Changing the selections in 'Dataset' and 'Filters' sections");
                    log.error("[Preview-Simple-End],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:query,code:query-data-empty");
                    return response;
                }

                //Validating the analytics method
                try {
                    String methodValidJSON = performPutRequest(baseUrl + "/AnalyticsMethod/AnalyticsMethods/" + previewRequest.getAnalyticsMethodId().get("0") + "/validateConfiguration", queryToMethodConfig.toString());


                    OpenLAPDataSetConfigValidationResult methodValid = mapper.readValue(methodValidJSON, OpenLAPDataSetConfigValidationResult.class);

                    if (!methodValid.isValid()) {
                        response.setSuccess(false);
                        response.setErrorMessage(methodValid.getValidationMessage());
                        log.error("[Preview-Simple-End],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:method-validate,code:unknown,msg:"+methodValid.getValidationMessage());
                        return response;
                    }

                } catch (Exception exc) {
                    response.setSuccess(false);
                    response.setErrorMessage("An unknown error occurred while validating the inputs to analysis. please contact the admins with the following error: " + exc.getMessage());
                    log.error("[Preview-Simple-End],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:method-validate,code:unknown,msg:"+exc.getMessage());
                    return response;
                }

                //Applying the analytics method
                OpenLAPDataSet analyzedDataSet = null;
                try {
                    AnalyticsMethodsClassPathLoader classloaderPath = analyticsMethodsService.getFolderNameFromResources();
                    AnalyticsMethod method = analyticsMethodsService.loadAnalyticsMethodInstance(previewRequest.getAnalyticsMethodId().get("0"),classloaderPath);
                    String rawMethodParams = previewRequest.getMethodInputParams().containsKey("0") ? previewRequest.getMethodInputParams().get("0") : "";
                    Map<String, String> methodParams = rawMethodParams.isEmpty() ? new HashMap<>() : mapper.readValue(rawMethodParams, new TypeReference<HashMap<String,String>>() {});

                    method.initialize(queryDataSet, queryToMethodConfig, methodParams);
                    System.out.println(queryDataSet);
                    analyzedDataSet = method.execute();
                } catch (AnalyticsMethodInitializationException amexc) {
                    response.setSuccess(false);
                    response.setErrorMessage("An unknown error occurred while starting the analysis. please contact the admins with the following error: " + amexc.getMessage());
                    log.error("[Preview-Simple-End],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:method-initialize,code:unknown,msg:"+amexc.getMessage());
                    return response;
                } catch (Exception exc) {
                    response.setSuccess(false);
                    response.setErrorMessage("An unknown error occurred while performing the analysis. please contact the admins with the following error: " +exc.getMessage());
                    log.error("[Preview-Simple-End],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:method-execute,code:unknown,msg:"+exc.getMessage());
                    return response;
                }


                if (analyzedDataSet == null) {
                    response.setSuccess(false);
                    response.setErrorMessage("No analyzed data was generated from the analytics method");
                    log.error("[Preview-Simple-End],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:analysis,code:analysis-data-empty");
                    return response;
                }

                //try { log.info("Analyzed data: " + mapper.writeValueAsString(analyzedDataSet)); } catch (Exception exc) { }


                //visualizing the analyzed data
                String indicatorCode = "";
                OpenLAPPortConfig methodToVisConfig = previewRequest.getMethodToVisualizationConfig();


                //Validating the visualization technique
                try {
                    ValidateVisualizationTypeConfigurationRequest visRequest = new ValidateVisualizationTypeConfigurationRequest();
                    visRequest.setConfigurationMapping(methodToVisConfig);
                    String visRequestJSON = mapper.writeValueAsString(visRequest);

                    ValidateVisualizationTypeConfigurationResponse visValid = performJSONPostRequest(baseUrl + "/frameworks/" + previewRequest.getVisualizationLibraryId() + "/methods/" + previewRequest.getVisualizationTypeId() + "/validateConfiguration", visRequestJSON, ValidateVisualizationTypeConfigurationResponse.class);

                    if (!visValid.isConfigurationValid()){
                        response.setSuccess(false);
                        response.setErrorMessage(visValid.getValidationMessage());
                        log.error("[Preview-Simple-End],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:vis-validate,code:unknown,msg:"+visValid.getValidationMessage());
                        return response;
                    }

                } catch (Exception exc) {
                    response.setSuccess(false);
                    response.setErrorMessage(exc.getMessage());
                    log.error("[Preview-Simple-End],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:vis-validate,code:unknown,msg:"+exc.getMessage());
                    return response;
                }


                try {
                    GenerateVisualizationCodeRequest visualRequest = new GenerateVisualizationCodeRequest();
                    visualRequest.setLibraryId(previewRequest.getVisualizationLibraryId());
                    visualRequest.setTypeId(previewRequest.getVisualizationTypeId());
                    visualRequest.setDataSet(analyzedDataSet);
                    visualRequest.setPortConfiguration(methodToVisConfig);
                    visualRequest.setParams(previewRequest.getAdditionalParams());

                    String visualRequestJSON = mapper.writeValueAsString(visualRequest);

                    GenerateVisualizationCodeResponse visualResponse = performJSONPostRequest(baseUrl + "/generateVisualizationCode", visualRequestJSON, GenerateVisualizationCodeResponse.class);

                    indicatorCode = visualResponse.getVisualizationCode();
                } catch (Exception exc) {
                    response.setSuccess(false);
                    response.setErrorMessage(exc.getMessage());
                    log.error("[Preview-Simple-End],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:vis-execute,code:unknown,msg:"+exc.getMessage());
                    return response;
                }

                response.setSuccess(true);
                response.setErrorMessage("");
                response.setVisualizationCode(encodeURIComponent(indicatorCode));

            log.info("[Preview-Simple-End],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime));

            return response;
        } catch (Exception exc) {
            response.setSuccess(false);
            response.setErrorMessage(exc.getMessage());
            log.error("[Preview-Simple-End],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:indicator-preview,code:unknown,msg:"+exc.getMessage());
            return response;
        }
    }

    public IndicatorPreviewResponse getCompIndicatorPreview(IndicatorPreviewRequest previewRequest, String baseUrl) {
        long indicatorExecutionStartTime = System.currentTimeMillis();
        long localPreviewCount = previewCount++;

        IndicatorPreviewResponse response = new IndicatorPreviewResponse();
        try {
            ObjectMapper mapper = new ObjectMapper();

            try {
                log.info("[Preview-Composite-Start],user:"+previewRequest.getAdditionalParams().get("uid").toString()+",count:"+localPreviewCount+",param:" + mapper.writeValueAsString(previewRequest));
            } catch (Exception exc) { }

            if(previewRequest.getIndicatorType().equals("composite")) {

                Set<String> indicatorNames = previewRequest.getQueries().keySet();
                OpenLAPPortConfig methodToVisConfig = previewRequest.getMethodToVisualizationConfig();

                boolean addIndicatorNameColumn = false;
                String columnId = null;
                for(OpenLAPColumnConfigData outputConfig : methodToVisConfig.getOutputColumnConfigurationData()){
                    if(outputConfig.getId().equals("indicator_names"))
                        addIndicatorNameColumn = true;
                    else
                        columnId = outputConfig.getId();
                }

                OpenLAPDataSet combinedAnalyzedDataSet = null;

                for(String indicatorName: indicatorNames){


                    QueryParameters curIndQuery = previewRequest.getQueries().get(indicatorName);
                    OpenLAPPortConfig queryToMethodConfig = previewRequest.getQueryToMethodConfig().get(indicatorName);

                    if(curIndQuery != null) {
                        if(!previewRequest.getAdditionalParams().containsKey("rid")){
                            response.setSuccess(false);
                            response.setErrorMessage("This indicator require a user code to generate personalized data which is not provided. Try again after refreshing the web page.");
                            log.error("[Preview-Composite-End]["+indicatorName+"],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:rid,code:rid-missing");
                            return response;
                        }
                        else{
                            String userID = previewRequest.getAdditionalParams().get("rid").toString().toUpperCase();
                           // curIndQuery = curIndQuery.replace("xxxridxxx", userID);
                        }
                    }

                    //OpenLAPDataSet queryDataSet = executeIndicatorQuery(previewRequest, queryToMethodConfig, 0);
                    OpenLAPDataSet queryDataSet = statementServiceImp.getAllStatementsByCustomQuery(organizationId, lrsId, curIndQuery);

                    if (queryDataSet == null) {
                        response.setSuccess(false);
                        response.setErrorMessage("No data found for the indicator '" + indicatorName + "'. Try selecting some other indicator for composite which can be previewed");
                        log.error("[Preview-Composite-End]["+indicatorName+"],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:query,code:query-data-empty");
                        return response;
                    }

                    //try { log.info("Query data: " + mapper.writeValueAsString(queryDataSet)); } catch (Exception exc) {}

                    //Validating the analytics method
                    try {
                        String methodValidJSON = performPutRequest(baseUrl + "/AnalyticsMethods/" + previewRequest.getAnalyticsMethodId().get(indicatorName) + "/validateConfiguration", queryToMethodConfig.toString());
                        OpenLAPDataSetConfigValidationResult methodValid = mapper.readValue(methodValidJSON, OpenLAPDataSetConfigValidationResult.class);

                        if (!methodValid.isValid()) {
                            response.setSuccess(false);
                            response.setErrorMessage(methodValid.getValidationMessage());
                            log.error("[Preview-Composite-End]["+indicatorName+"],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:method-validate,code:unknown,msg:"+methodValid.getValidationMessage());
                            return response;
                        }

                    } catch (Exception exc) {
                        response.setSuccess(false);
                        response.setErrorMessage("An unknown error has occurred while validating the inputs to the analysis for indicator '" + indicatorName + "'. please contact the admins with the following error: " +exc.getMessage());
                        log.error("[Preview-Composite-End]["+indicatorName+"],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:method-validate,code:unknown,msg:"+exc.getMessage());
                        return response;
                    }

                    //Applying the analytics method
                    OpenLAPDataSet analyzedDataSet = null;
                    try {
                        AnalyticsMethodsClassPathLoader classPathLoader = analyticsMethodsService.getFolderNameFromResources();

                        AnalyticsMethod method = analyticsMethodsService.loadAnalyticsMethodInstance(previewRequest.getAnalyticsMethodId().get(indicatorName), classPathLoader);
                        String rawMethodParams = previewRequest.getMethodInputParams().containsKey(indicatorName) ? previewRequest.getMethodInputParams().get(indicatorName) : "";

                        Map<String, String> methodParams = rawMethodParams.isEmpty() ? new HashMap<>() : mapper.readValue(rawMethodParams, new TypeReference<HashMap<String,String>>() {});
                        method.initialize(queryDataSet, queryToMethodConfig, methodParams);
                        analyzedDataSet = method.execute();
                    } catch (AnalyticsMethodInitializationException amexc) {
                        response.setSuccess(false);
                        response.setErrorMessage("An unknown error occurred while starting the analysis for the indicator '" + indicatorName + "'. please contact the admins with the following error: " + amexc.getMessage());
                        log.error("[Preview-Composite-End]["+indicatorName+"],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:method-initialize,code:unknown,msg:"+amexc.getMessage());
                        return response;
                    } catch (Exception exc) {
                        response.setSuccess(false);
                        response.setErrorMessage("An unknown error occurred while performing the analysis for the indicator '" + indicatorName + "'. please contact the admins with the following error: " + exc.getMessage());
                        log.error("[Preview-Composite-End]["+indicatorName+"],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:method-execute,code:unknown,msg:"+exc.getMessage());
                        return response;
                    }


                    if (analyzedDataSet == null) {
                        response.setSuccess(false);
                        response.setErrorMessage("No analyzed data was generated for the indicator '" + indicatorName + "'.");
                        log.error("[Preview-Composite-End]["+indicatorName+"],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:analysis,code:analysis-data-empty");
                        return response;
                    }

                    //try { log.info("Analyzed data: " + mapper.writeValueAsString(analyzedDataSet)); } catch (Exception exc) {}

                    //Merging analyzed dataset
                    if(combinedAnalyzedDataSet == null) {
                        combinedAnalyzedDataSet = analyzedDataSet;

                        if(addIndicatorNameColumn)
                            if(!combinedAnalyzedDataSet.getColumns().containsKey("indicator_names"))
                                combinedAnalyzedDataSet.addOpenLAPDataColumn(OpenLAPDataColumnFactory.createOpenLAPDataColumnOfType("indicator_names", OpenLAPColumnDataType.Text, true, "Indicator Names", "Names of the indicators combines together to form the composite."));
                    }
                    else {
                        List<OpenLAPColumnConfigData> columnConfigDatas = analyzedDataSet.getColumnsConfigurationData();

                        for(OpenLAPColumnConfigData columnConfigData : columnConfigDatas)
                            combinedAnalyzedDataSet.getColumns().get(columnConfigData.getId()).getData().addAll(analyzedDataSet.getColumns().get(columnConfigData.getId()).getData());
                    }

                    if(addIndicatorNameColumn) {
                        int dataSize = analyzedDataSet.getColumns().get(columnId).getData().size();

                        for(int i=0;i<dataSize;i++)
                            combinedAnalyzedDataSet.getColumns().get("indicator_names").getData().add(indicatorName);
                    }
                }

                //try { log.info("Combined Analyzed data: " + mapper.writeValueAsString(combinedAnalyzedDataSet)); } catch (Exception exc) {}

                //visualizing the analyzed data
                String indicatorCode = "";

                //Validating the visualization technique
                try {

                    ValidateVisualizationTypeConfigurationRequest visRequest = new ValidateVisualizationTypeConfigurationRequest();
                    visRequest.setConfigurationMapping(methodToVisConfig);

                    String visRequestJSON = mapper.writeValueAsString(visRequest);

                    ValidateVisualizationTypeConfigurationResponse visValid = performJSONPostRequest(baseUrl + "/frameworks/" + previewRequest.getVisualizationLibraryId() + "/methods/" + previewRequest.getVisualizationTypeId() + "/validateConfiguration", visRequestJSON, ValidateVisualizationTypeConfigurationResponse.class);

                    if (!visValid.isConfigurationValid()){
                        response.setSuccess(false);
                        response.setErrorMessage(visValid.getValidationMessage());
                        log.error("[Preview-Composite-End],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:vis-validate,code:unknown,msg:"+visValid.getValidationMessage());
                        return response;
                    }

                } catch (Exception exc) {
                    response.setSuccess(false);
                    response.setErrorMessage(exc.getMessage());
                    log.error("[Preview-Composite-End],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:vis-validate,code:unknown,msg:"+exc.getMessage());
                    return response;
                }

                try {
                    GenerateVisualizationCodeRequest visualRequest = new GenerateVisualizationCodeRequest();
                    visualRequest.setLibraryId(previewRequest.getVisualizationLibraryId());
                    visualRequest.setTypeId(previewRequest.getVisualizationTypeId());
                    visualRequest.setDataSet(combinedAnalyzedDataSet);
                    visualRequest.setPortConfiguration(methodToVisConfig);
                    visualRequest.setParams(previewRequest.getAdditionalParams());

                    String visualRequestJSON = mapper.writeValueAsString(visualRequest);

                    GenerateVisualizationCodeResponse visualResponse = performJSONPostRequest(baseUrl + "/generateVisualizationCode", visualRequestJSON, GenerateVisualizationCodeResponse.class);

                    indicatorCode = visualResponse.getVisualizationCode();
                } catch (Exception exc) {
                    response.setSuccess(false);
                    response.setErrorMessage(exc.getMessage());
                    log.error("[Preview-Composite-End],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:vis-execute,code:unknown,msg:"+exc.getMessage());
                    return response;
                }

                response.setSuccess(true);
                response.setErrorMessage("");
                response.setVisualizationCode(encodeURIComponent(indicatorCode));
            }
            log.info("[Preview-Composite-End],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime));
            return response;
        } catch (Exception exc) {
            response.setSuccess(false);
            response.setErrorMessage(exc.getMessage());
            log.error("[Preview-Composite-End],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:indicator-preview,code:unknown,msg:"+exc.getMessage());
            return response;
        }
    }

    public IndicatorPreviewResponse getMLAIIndicatorPreview(IndicatorPreviewRequest previewRequest, String baseUrl) {
        long indicatorExecutionStartTime = System.currentTimeMillis();
        long localPreviewCount = previewCount++;

        IndicatorPreviewResponse response = new IndicatorPreviewResponse();
        try {
            ObjectMapper mapper = new ObjectMapper();

            try {
                log.info("[Preview-MLAI-Start],user:"+previewRequest.getAdditionalParams().get("uid").toString()+",count:"+localPreviewCount+",param:" + mapper.writeValueAsString(previewRequest));
            } catch (Exception exc) { }

            if(previewRequest.getIndicatorType().equals("multianalysis")) {

                Set<String> datasetIds = previewRequest.getQueries().keySet();
                OpenLAPPortConfig methodToVisConfig = previewRequest.getMethodToVisualizationConfig();

                Map<String, OpenLAPDataSet> analyzedDatasetMap = new HashMap<>();

                for(String datasetId: datasetIds){

                    if(datasetId.equals("0")) // skipping 0 since it is the id for the 2nd level analytics method and it does not have a query
                        continue;

                    QueryParameters curIndQuery = previewRequest.getQueries().get(datasetId);
                    OpenLAPPortConfig queryToMethodConfig = previewRequest.getQueryToMethodConfig().get(datasetId);
/*
                    if(curIndQuery.contains("xxxridxxx")) {
                        if(!previewRequest.getAdditionalParams().containsKey("rid")){
                            response.setSuccess(false);
                            response.setErrorMessage("This indicator require a user code to generate personalized data which is not provided. Try again after refreshing the web page.");
                            log.error("[Preview-MLAI-End]["+datasetId+"],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:rid,code:rid-missing");
                            return response;
                        }
                        else{
                            String userID = previewRequest.getAdditionalParams().get("rid").toString().toUpperCase();
                            curIndQuery = curIndQuery.replace("xxxridxxx", userID);
                        }
                    }
*/
                   // OpenLAPDataSet queryDataSet = executeIndicatorQuery(curIndQuery, queryToMethodConfig, 0);
                    OpenLAPDataSet queryDataSet = statementServiceImp.getAllStatementsByCustomQuery(organizationId, lrsId, curIndQuery);
                    if (queryDataSet == null) {
                        response.setSuccess(false);
                        response.setErrorMessage("No data found for the dataset '" + datasetId + "'. Try Changing the selections in its 'Dataset' and 'Filters' sections");
                        log.error("[Preview-MLAI-End]["+datasetId+"],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:query,code:query-data-empty");
                        return response;
                    }

                    //try { log.info("Query data: " + mapper.writeValueAsString(queryDataSet)); } catch (Exception exc) {}


                    //Validating the analytics method
                    try {
                        String methodValidJSON = performPutRequest(baseUrl + "/AnalyticsMethods/" + previewRequest.getAnalyticsMethodId().get(datasetId) + "/validateConfiguration", queryToMethodConfig.toString());
                        OpenLAPDataSetConfigValidationResult methodValid = mapper.readValue(methodValidJSON, OpenLAPDataSetConfigValidationResult.class);
                        if (!methodValid.isValid()) {
                            response.setSuccess(false);
                            response.setErrorMessage(methodValid.getValidationMessage());
                            log.error("[Preview-MLAI-End]["+datasetId+"],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:method-validate,code:unknown,msg:"+methodValid.getValidationMessage());
                            return response;
                        }
                    } catch (Exception exc) {
                        response.setSuccess(false);
                        response.setErrorMessage("An unknown error has occurred while validating the inputs to analysis for dataset '" + datasetId + "'. please contact the admins with the following error: " + exc.getMessage());
                        log.error("[Preview-MLAI-End]["+datasetId+"],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:method-validate,code:unknown,msg:"+exc.getMessage());
                        return response;
                    }

                    //Applying the analytics method
                    OpenLAPDataSet singleAnalyzedDataSet = null;
                    try {
                        AnalyticsMethodsClassPathLoader classPathLoader = analyticsMethodsService.getFolderNameFromResources();
                        AnalyticsMethod method = analyticsMethodsService.loadAnalyticsMethodInstance(previewRequest.getAnalyticsMethodId().get(datasetId), classPathLoader);
                        String rawMethodParams = previewRequest.getMethodInputParams().containsKey(datasetId) ? previewRequest.getMethodInputParams().get(datasetId) : "";
                        Map<String, String> methodParams = rawMethodParams.isEmpty() ? new HashMap<>() : mapper.readValue(rawMethodParams, new TypeReference<HashMap<String,String>>() {});

                        method.initialize(queryDataSet, queryToMethodConfig, methodParams);
                        singleAnalyzedDataSet = method.execute();
                    } catch (AnalyticsMethodInitializationException ame) {
                        response.setSuccess(false);
                        response.setErrorMessage("An unknown error occurred while starting the analysis for the dataset '" + datasetId + "'. please contact the admins with the following error: " + ame.getMessage());
                        log.error("[Preview-MLAI-End]["+datasetId+"],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:method-initialize,code:unknown,msg:"+ame.getMessage());
                        return response;
                    } catch (Exception exc) {
                        response.setSuccess(false);
                        response.setErrorMessage("An unknown error occurred while performing the analysis for he dataset '" + datasetId + "'. please contact the admins with the following error: " + exc.getMessage());
                        log.error("[Preview-MLAI-End]["+datasetId+"],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:method-execute,code:unknown,msg:"+exc.getMessage());
                        return response;
                    }

                    if (singleAnalyzedDataSet == null) {
                        response.setSuccess(false);
                        response.setErrorMessage("No analyzed data is generated from the dataset '" + datasetId + "'.");
                        log.error("[Preview-MLAI-End]["+datasetId+"],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:analysis,code:analysis-data-empty");
                        return response;
                    }

                    analyzedDatasetMap.put(datasetId, singleAnalyzedDataSet);
                }

                //mergning dataset
                List<OpenLAPDataSetMergeMapping> mergeMappings = previewRequest.getDataSetMergeMappingList();
                OpenLAPDataSet mergedDataset = null;
                String mergeStatus = "";

                try {
                    while (mergeMappings.size() > 0) {
                        OpenLAPDataSet firstDataset = null;
                        OpenLAPDataSet secondDataset = null;

                        for (OpenLAPDataSetMergeMapping mergeMapping : mergeMappings) {
                            String key1 = mergeMapping.getIndRefKey1();
                            String key2 = mergeMapping.getIndRefKey2();

                            int dashCountKey1 = StringUtils.countMatches(key1, "-"); //Merged keys will always be in key1
                            //int dashCountKey2 = StringUtils.countMatches(key2, "-");

                            if (dashCountKey1 == 0) {
                                firstDataset = analyzedDatasetMap.get(key1);
                                secondDataset = analyzedDatasetMap.get(key2);
                            } else {
                                if (!mergeStatus.isEmpty() && key1.equals(mergeStatus)) {
                                    firstDataset = mergedDataset;
                                    secondDataset = analyzedDatasetMap.get(key2);
                                    key1 = "(" + key1 + ")";
                                } else
                                    continue;
                            }
                            OpenLAPDataSet processedDataset = mergeOpenLAPDataSets(firstDataset, secondDataset, mergeMapping);
                            if (processedDataset != null) {
                                mergedDataset = processedDataset;
                                mergeStatus = key1 + "-" + key2;
                                mergeMappings.remove(mergeMapping);
                            }
                            break;
                        }
                    }
                }
                catch (Exception exc){
                    response.setSuccess(false);
                    response.setErrorMessage("An unknown error occurred while mergning the dataset. please contact the admins with the following error: " + exc.getMessage());
                    log.error("[Preview-MLAI-End],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:dataset-merge,code:unknown,msg:"+exc.getMessage());
                    return response;
                }

                //try { log.info("Merged Analyzed data: " + mapper.writeValueAsString(mergedDataset)); } catch (Exception exc) {}


                OpenLAPDataSet analyzedDataSet = null;

                //Applying the final analysis whose configuration is stored always with id "0"
                OpenLAPPortConfig finalQueryToMethodConfig = previewRequest.getQueryToMethodConfig().get("0");
                try {
                    AnalyticsMethodsClassPathLoader classPathLoader = analyticsMethodsService.getFolderNameFromResources();
                    AnalyticsMethod method = analyticsMethodsService.loadAnalyticsMethodInstance(previewRequest.getAnalyticsMethodId().get("0"),classPathLoader);
                    String rawMethodParams = previewRequest.getMethodInputParams().containsKey("0")?previewRequest.getMethodInputParams().get("0"):"";
                    Map<String, String> methodParams = rawMethodParams.isEmpty() ? new HashMap<>() : mapper.readValue(rawMethodParams, new TypeReference<HashMap<String,String>>() {});

                    method.initialize(mergedDataset, finalQueryToMethodConfig, methodParams);
                    analyzedDataSet = method.execute();
                } catch (AnalyticsMethodInitializationException amexc) {
                    response.setSuccess(false);
                    response.setErrorMessage("An unknown error occurred while starting the final analysis. please contact the admins with the following error: " + amexc.getMessage());
                    log.error("[Preview-MLAI-End][final],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:method-initialize,code:unknown,msg:"+amexc.getMessage());
                    return response;
                } catch (Exception exc) {
                    response.setSuccess(false);
                    response.setErrorMessage("An unknown error occurred while performing the final analysis. please contact the admins with the following error: " + exc.getMessage());
                    log.error("[Preview-MLAI-End][final],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:method-execute,code:unknown,msg:"+exc.getMessage());
                    return response;
                }

                if (analyzedDataSet == null) {
                    response.setSuccess(false);
                    response.setErrorMessage("No analyzed data is generated from the final analysis.");
                    log.error("[Preview-MLAI-End][final],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:analysis,code:analysis-data-empty");
                    return response;
                }

                //try { log.info("Analyzed data: " + mapper.writeValueAsString(analyzedDataSet)); } catch (Exception exc) { }



                //visualizing the analyzed data
                String indicatorCode = "";

                //Validating the visualization technique
                try {
                    ValidateVisualizationTypeConfigurationRequest visRequest = new ValidateVisualizationTypeConfigurationRequest();
                    visRequest.setConfigurationMapping(methodToVisConfig);
                    String visRequestJSON = mapper.writeValueAsString(visRequest);
                    ValidateVisualizationTypeConfigurationResponse visValid = performJSONPostRequest(baseUrl + "/frameworks/" + previewRequest.getVisualizationLibraryId() + "/methods/" + previewRequest.getVisualizationTypeId() + "/validateConfiguration", visRequestJSON, ValidateVisualizationTypeConfigurationResponse.class);

                    if (!visValid.isConfigurationValid()) {
                        response.setSuccess(false);
                        response.setErrorMessage(visValid.getValidationMessage());
                        log.error("[Preview-MLAI-End],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:vis-validate,code:unknown,msg:"+visValid.getValidationMessage());
                        return response;
                    }

                } catch (Exception exc) {
                    response.setSuccess(false);
                    response.setErrorMessage(exc.getMessage());
                    log.error("[Preview-MLAI-End],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:vis-validate,code:unknown,msg:"+exc.getMessage());
                    return response;
                }


                try {
                    GenerateVisualizationCodeRequest visualRequest = new GenerateVisualizationCodeRequest();
                    visualRequest.setLibraryId(previewRequest.getVisualizationLibraryId());
                    visualRequest.setTypeId(previewRequest.getVisualizationTypeId());
                    visualRequest.setDataSet(analyzedDataSet);
                    visualRequest.setPortConfiguration(methodToVisConfig);
                    visualRequest.setParams(previewRequest.getAdditionalParams());

                    String visualRequestJSON = mapper.writeValueAsString(visualRequest);

                    GenerateVisualizationCodeResponse visualResponse = performJSONPostRequest(baseUrl + "/generateVisualizationCode", visualRequestJSON, GenerateVisualizationCodeResponse.class);

                    indicatorCode = visualResponse.getVisualizationCode();
                } catch (Exception exc) {
                    response.setSuccess(false);
                    response.setErrorMessage(exc.getMessage());
                    log.error("[Preview-MLAI-End],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:vis-execute,code:unknown,msg:"+exc.getMessage());
                    return response;
                }

                response.setSuccess(true);
                response.setErrorMessage("");
                response.setVisualizationCode(encodeURIComponent(indicatorCode));
            }
            log.info("[Preview-MLAI-End],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime));
            return response;
        } catch (Exception exc) {
            response.setSuccess(false);
            response.setErrorMessage(exc.getMessage());
            log.error("[Preview-MLAI-End],count:"+localPreviewCount+",time:" + (System.currentTimeMillis()-indicatorExecutionStartTime)+",step:indicator-preview,code:unknown,msg:"+exc.getMessage());
            return response;
        }
    }

    //endregion
    public OpenLAPDataSet mergeOpenLAPDataSets(OpenLAPDataSet firstDataset, OpenLAPDataSet secondDataSet, OpenLAPDataSetMergeMapping mapping){

        int firstDataSize, secondDataSize;

        int intDefaultValue = -1;
        String strDefaultValue = "-";
        char charDefaultValue = '-';

        List<OpenLAPDataColumn> firstColumns = firstDataset.getColumnsAsList(false);
        HashMap<String, ArrayList> firstData = new HashMap<>();
        firstDataSize = firstColumns.get(0).getData().size();

        for(OpenLAPDataColumn column : firstColumns)
            firstData.put(column.getConfigurationData().getId(), column.getData());

        List<OpenLAPDataColumn> firstColumnsBase = firstDataset.getColumnsAsList(false);

        List<OpenLAPDataColumn> secondColumns = secondDataSet.getColumnsAsList(false);
        HashMap<String, ArrayList> secondData = new HashMap<>();
        secondDataSize = secondColumns.get(0).getData().size();

        for(OpenLAPDataColumn column : secondColumns) {
            secondData.put(column.getConfigurationData().getId(), column.getData());

            //Adding the second dataset columns with default values to first dataset
            if(!mapping.getIndRefField2().equals(column.getConfigurationData().getId())){

                OpenLAPDataColumn newColumn;
                ArrayList newData;
                switch (column.getConfigurationData().getType())
                {
                    case Text:
                        newColumn = new OpenLAPDataColumn<String>(column.getConfigurationData().getId(), OpenLAPColumnDataType.Text, column.getConfigurationData().isRequired(), column.getConfigurationData().getTitle(), column.getConfigurationData().getDescription());
                        newData = new ArrayList<String>(Collections.nCopies(firstDataSize, strDefaultValue));
                        break;
                    case TrueFalse:
                        newColumn = new OpenLAPDataColumn<Boolean>(column.getConfigurationData().getId(), OpenLAPColumnDataType.TrueFalse, column.getConfigurationData().isRequired(), column.getConfigurationData().getTitle(), column.getConfigurationData().getDescription());
                        newData = new ArrayList<Boolean>(Collections.nCopies(firstDataSize, false));
                        break;
                    case Numeric:
                        newColumn = new OpenLAPDataColumn<Float>(column.getConfigurationData().getId(), OpenLAPColumnDataType.Numeric, column.getConfigurationData().isRequired(), column.getConfigurationData().getTitle(), column.getConfigurationData().getDescription());
                        newData = new ArrayList<Float>(Collections.nCopies(firstDataSize, (float)intDefaultValue));
                        break;
                    default:
                        newColumn = new OpenLAPDataColumn<String>(column.getConfigurationData().getId(), OpenLAPColumnDataType.Text, column.getConfigurationData().isRequired(), column.getConfigurationData().getTitle(), column.getConfigurationData().getDescription());
                        newData = new ArrayList<String>(Collections.nCopies(firstDataSize, strDefaultValue));
                        break;
                }
                //newColumn.setData(newData);

                firstData.put(newColumn.getConfigurationData().getId(), newData);
                firstColumns.add(newColumn);
            }
        }


        //merging data from second dataset into first dataset

        for(int i=0;i<secondDataSize;i++){
            Object commonFieldValue = secondData.get(mapping.getIndRefField2()).get(i);

            boolean valFound = false;
            for(int j=0;j<firstDataSize;j++) {
                if(firstData.get(mapping.getIndRefField1()).get(j).equals(commonFieldValue)){

                    for(String key : secondData.keySet()) {
                        if(!key.equals(mapping.getIndRefField2()))
                            firstData.get(key).set(j, secondData.get(key).get(i));
                    }

                    valFound = true;
                    break;
                }
            }

            if(!valFound){
                for(String key : secondData.keySet()) {
                    if(!key.equals(mapping.getIndRefField2()))
                        firstData.get(key).add(secondData.get(key).get(i));
                }

                //Adding the default values to the first dataset data array
                for(OpenLAPDataColumn column : firstColumnsBase){
                    if(column.getConfigurationData().getId().equals(mapping.getIndRefField1()))
                        firstData.get(column.getConfigurationData().getId()).add(commonFieldValue);
                    else{
                        switch (column.getConfigurationData().getType())
                        {
                            case Text:
                                firstData.get(column.getConfigurationData().getId()).add(strDefaultValue);
                                break;
                            case TrueFalse:
                                firstData.get(column.getConfigurationData().getId()).add(false);
                                break;
                            case Numeric:
                                firstData.get(column.getConfigurationData().getId()).add((float)intDefaultValue);
                                break;
                            default:
                                firstData.get(column.getConfigurationData().getId()).add(strDefaultValue);
                                break;
                        }
                    }
                }
            }
        }

        OpenLAPDataSet mergedDataset = new OpenLAPDataSet();
        //Generating merged dataset using the columns and data
        for(OpenLAPDataColumn column : firstColumns){
            column.setData(firstData.get(column.getConfigurationData().getId()));
            try {
                mergedDataset.addOpenLAPDataColumn(column);
            } catch (OpenLAPDataColumnException e) {
                e.printStackTrace();
            }
        }

        return mergedDataset;
    }

    public List<Indicator> getAllIndicators() {
        String query ="From Indicator";
        List<Indicator> result = em.createQuery(query).getResultList();
        return result;
    }

    public void deleteIndicator(String indicatorId) {
        Indicator result = em.find(Indicator.class, indicatorId);
        if (result == null || indicatorId == null) {
            throw new ItemNotFoundException("Indicator with id = {" + indicatorId + "} not found.","2");
        }else {
            em.getTransaction().begin();
            em.remove(result);
            em.getTransaction().commit();
            em.close();
        }
    }

    public TriadCache getCacheByTriadId(String triadId, String userHash) throws ItemNotFoundException {

        String query = "From TriadCache where id =: triadId";
        List<TriadCache> result = em.createQuery(query, TriadCache.class).getResultList();

        if (result == null) {
            return null;
        } else {
            if(result.size()==1){
                if(result.get(0).getUserHash()==null || result.get(0).getUserHash().equals("") || result.get(0).getUserHash().equals(userHash))
                    return result.get(0);
                else
                    return null;
            }
            else{
                for(TriadCache triadCache:result)
                    if(triadCache.getUserHash().equals(userHash))
                        return triadCache;

                return null;
            }
        }
    }
    public IndicatorResponse getTriadById(String triadId, HttpServletRequest request) {
        IndicatorResponse indicatorResponse = new IndicatorResponse();
        String baseUrl = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());
        ObjectMapper mapper = new ObjectMapper();

        Triad triad;

        try {
            String triadJSON = performGetRequest(baseUrl + "/AnalyticsModules/Triads/" + triadId);
            triad = mapper.readValue(triadJSON, Triad.class);
        } catch (Exception exc) {
            throw new ItemNotFoundException("Indicator with triad id '" + triadId + "' not found.", "1");
        }


        if (triad == null)
            throw new ItemNotFoundException("Indicator with triad id '" + triadId + "' not found.","1");


        Indicator indicator = getIndicatorById(triad.getIndicatorReference().getIndicators().get("0").getId());

        indicatorResponse.setId(triad.getId());
        //indicatorResponse.setQuery(indicator.getQuery());
        indicatorResponse.setIndicatorReference(triad.getIndicatorReference());
        indicatorResponse.setAnalyticsMethodReference(triad.getAnalyticsMethodReference());
        indicatorResponse.setVisualizationReference(triad.getVisualizationReference());
        //indicatorResponse.setQueryToMethodConfig(triad.getIndicatorToAnalyticsMethodMapping().getPortConfigs().get("0"));
        //indicatorResponse.setMethodToVisualizationConfig(triad.getAnalyticsMethodToVisualizationMapping());
        indicatorResponse.setName(indicator.getName());
        indicatorResponse.setParameters(triad.getParameters());
        //indicatorResponse.setComposite(indicator.isComposite());
        indicatorResponse.setIndicatorType(triad.getIndicatorReference().getIndicatorType());
        indicatorResponse.setCreatedBy(triad.getCreatedBy());

        return indicatorResponse;
    }

    private boolean setQuestionTriadMapping(String questionId, String triadId){
        boolean isSuccess = true;

        String queryString = "INSERT INTO question_triad (question_id, triad_id)" +
                " VALUES ("+ questionId + "," + triadId + ")";

        try {
            em.getTransaction().begin();
            em.persist(queryString);
            em.flush();
            em.getTransaction().commit();
            //insertSQLQueryRaw(queryString);
        }
        catch (Exception exc){
            isSuccess = false;
        }

        return isSuccess;
    }

    public List<AnalyticsMethods> getAllAnalyticsMethods(HttpServletRequest request) {
        String baseUrl = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());

        ObjectMapper mapper = new ObjectMapper();

        List<AnalyticsMethods> allMethods;

        try {
            String methodsJSON = performGetRequest(baseUrl + "/AnalyticsMethod/AnalyticsMethods");
            allMethods = mapper.readValue(methodsJSON, mapper.getTypeFactory().constructCollectionType(List.class, AnalyticsMethods.class));
        } catch (Exception exc) {
            System.out.println(exc.getMessage());
            return null;
        }

        return allMethods;
    }

    public VisualizationLibrariesDetailsResponse getAllVisualizations(HttpServletRequest request) {

        String baseUrl = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());
        ObjectMapper mapper = new ObjectMapper();

        VisualizationLibrariesDetailsResponse allVis;

        try {
            String visualizationsJSON = performGetRequest(baseUrl + "/frameworks/list");
            allVis = mapper.readValue(visualizationsJSON,  mapper.getTypeFactory().constructCollectionType(List.class, VisualizationLibrariesDetailsResponse.class));
        } catch (Exception exc) {
            System.out.println(exc.getMessage());
            return null;
        }

        return allVis;
    }

    public VisualizationLibraryDetailsResponse getVisualizationsMethods(String frameworkId, HttpServletRequest request) {
        String baseUrl = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());
        ObjectMapper mapper = new ObjectMapper();

        VisualizationLibraryDetailsResponse visMethods;

        try {
            String visualizationsJSON = performGetRequest(baseUrl + "/frameworks/" + frameworkId);
            visMethods = mapper.readValue(visualizationsJSON,  mapper.getTypeFactory().constructCollectionType(List.class, VisualizationLibraryDetailsResponse.class));
        } catch (Exception exc) {
            System.out.println(exc.getMessage());
            return null;
        }

        return visMethods;
    }

    public List<AnalyticsGoal> getAllGoals(HttpServletRequest request) {
        String baseUrl = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());

        ObjectMapper mapper = new ObjectMapper();

        List<AnalyticsGoal> allGoals;

        try {
            String goalsJSON = performGetRequest(baseUrl + "/AnalyticsModule/AnalyticsGoals/");
            allGoals = mapper.readValue(goalsJSON, mapper.getTypeFactory().constructCollectionType(List.class, AnalyticsGoal.class));
        } catch (Exception exc) {
            System.out.println(exc.getMessage());
            return null;
        }

        return allGoals;
    }

    public List<AnalyticsGoal> getActiveGoals(String userid, HttpServletRequest request) {
        if(userid!=null)
            log.info("[Editor-New],user:"+userid);
        String baseUrl = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());

        ObjectMapper mapper = new ObjectMapper();

        List<AnalyticsGoal> allGoals;

        try {
            String goalsJSON = performGetRequest(baseUrl + "/AnalyticsModules/ActiveAnalyticsGoals/");
            allGoals = mapper.readValue(goalsJSON, mapper.getTypeFactory().constructCollectionType(List.class, AnalyticsGoal.class));
        } catch (Exception exc) {
            System.out.println(exc.getMessage());
            return null;
        }

        return allGoals;
    }

    public AnalyticsGoal saveGoal(String goalName, String goalDesc, String goalAuthor, HttpServletRequest request) {
        String baseUrl = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());

        ObjectMapper mapper = new ObjectMapper();

        try {
            AnalyticsGoal newGoal = new AnalyticsGoal();
            newGoal.setName(goalName);
            newGoal.setDescription(goalDesc);
            newGoal.setAuthor(goalAuthor);

            String saveGoalRequestJSON = mapper.writeValueAsString(newGoal);

            AnalyticsGoal savedGoal = performJSONPostRequest(baseUrl + "/AnalyticsModules/AnalyticsGoals/", saveGoalRequestJSON, AnalyticsGoal.class);

            return savedGoal;
        } catch (Exception exc) {
            System.out.println(exc.getMessage());
            return null;
        }
    }

    public AnalyticsGoal setGoalStatus(String goalId, boolean isActive, HttpServletRequest request) {
        String baseUrl = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());

        ObjectMapper mapper = new ObjectMapper();

        String setStatus;

        if(isActive)
            setStatus = "activate";
        else
            setStatus = "deactivate";

        try {

            String saveGoalResponseJSON = performPutRequest(baseUrl + "/AnalyticsModules/AnalyticsGoals/"+goalId+"/"+setStatus, null);
            AnalyticsGoal returnedGoal = mapper.readValue(saveGoalResponseJSON, AnalyticsGoal.class);

            return returnedGoal;
        } catch (Exception exc) {
            System.out.println(exc.getMessage());
            return null;
        }
    }

    public QuestionSaveResponse saveQuestionAndIndicators(QuestionSaveRequest saveRequest, HttpServletRequest request) {
        String baseUrl = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());

        log.info("Saving question:" + saveRequest.getQuestion());

        ObjectMapper mapper = new ObjectMapper();

        TypeReference<HashMap<String,String>> hashMapType = new TypeReference<HashMap<String,String>>() {};

        QuestionSaveResponse questionSaveResponse = new QuestionSaveResponse();

        Set<Triad> triads = new HashSet<Triad>();

        List<IndicatorSaveResponse> indicatorResponses = new ArrayList<IndicatorSaveResponse>();

        Question question = new Question();
        question.setName(saveRequest.getQuestion());
        question.setIndicatorCount(saveRequest.getIndicators().size());

        Question savedQuestion = saveQuestion(question);

        for (IndicatorSaveRequest indicatorRequest : saveRequest.getIndicators()) {

            IndicatorSaveResponse indicatorResponse = new IndicatorSaveResponse();
            indicatorResponse.setIndicatorClientID(indicatorRequest.getIndicatorClientID());

            try {
                //Saving the indicator query
                Indicator ind = new Indicator();
                ind.setName(indicatorRequest.getName());

                IndicatorReference indicatorReference = new IndicatorReference();
                indicatorReference.setIndicatorType(indicatorRequest.getIndicatorType());

                Set<Map.Entry<String, QueryParameters>> querySet = indicatorRequest.getQueries().entrySet();

                for (Map.Entry<String, QueryParameters> indQuery : querySet)
                    ind.getQueries().put(indQuery.getKey(), String.valueOf(indQuery.getValue()));

                indicatorReference.setDataSetMergeMappingList(indicatorRequest.getDataSetMergeMappingList());

                AnalyticsMethodReference methodReference = new AnalyticsMethodReference();
                for (Map.Entry<String, String> methodId : indicatorRequest.getAnalyticsMethodId().entrySet()){
                    String methodParamRaw = indicatorRequest.getMethodInputParams().get(methodId.getKey());
                    HashMap<String,String> methodParam;
                    if(methodParamRaw == null || methodParamRaw.isEmpty())
                        methodParam = new HashMap<>();
                    else
                        methodParam = mapper.readValue(methodParamRaw, hashMapType);
                    methodReference.getAnalyticsMethods().put(methodId.getKey(), new AnalyticsMethodParam(methodId.getValue(), methodParam));
                }

                OpenLAPPortConfigReference queryToMethodReference = new OpenLAPPortConfigReference();
                for (Map.Entry<String, OpenLAPPortConfig> methodConfig : indicatorRequest.getQueryToMethodConfig().entrySet()) {

                    //Validating the data to method port configuration
                    String queryToMethodConfigJSON = methodConfig.getValue().toString();
                    String queryToMethodConfigValidJSON = performPutRequest(baseUrl + "/AnalyticsMethods/"+indicatorRequest.getAnalyticsMethodId().get(methodConfig.getKey())+"/validateConfiguration", queryToMethodConfigJSON);
                    OpenLAPDataSetConfigValidationResult queryToMethodConfigValid =  mapper.readValue(queryToMethodConfigValidJSON, OpenLAPDataSetConfigValidationResult.class);
                    if(!queryToMethodConfigValid.isValid()){
                        indicatorResponse.setIndicatorSaved(false);
                        indicatorResponse.setErrorMessage("Query to Method Port-Configuration is not valid: " + queryToMethodConfigValid.getValidationMessage());
                        indicatorResponses.add(indicatorResponse);
                        continue;
                    }

                    queryToMethodReference.getPortConfigs().put(methodConfig.getKey(), methodConfig.getValue());
                }
                VisualizerReference visualizerReference;
                String visFrameworkJSON = performGetRequest(baseUrl + "/frameworks/" + indicatorRequest.getVisualizationLibraryId());
                VisualizationLibraryDetailsResponse frameworkResponse = mapper.readValue(visFrameworkJSON, VisualizationLibraryDetailsResponse.class);
                String visMethodJSON = performGetRequest(baseUrl + "/frameworks/"+ indicatorRequest.getVisualizationLibraryId() + "/methods/"+ indicatorRequest.getVisualizationTypeId());
                VisualizationTypeDetailsResponse methodResponse = mapper.readValue(visMethodJSON, VisualizationTypeDetailsResponse.class);
                visualizerReference = new VisualizerReference(
                        frameworkResponse.getVisualizationLibrary().getId(),
                        methodResponse.getVisualizationType().getId(),
                        indicatorRequest.getVisualizationInputParams());

                //Validating the method to visualization port configuration
                ValidateVisualizationTypeConfigurationRequest methodToVisConfigRequest = new ValidateVisualizationTypeConfigurationRequest();
                methodToVisConfigRequest.setConfigurationMapping(indicatorRequest.getMethodToVisualizationConfig());

                ValidateVisualizationTypeConfigurationResponse methodToVisConfigValid = performJSONPostRequest(baseUrl + "/frameworks/"+ indicatorRequest.getVisualizationLibraryId() + "/methods/"+ indicatorRequest.getVisualizationTypeId() +"/validateConfiguration", mapper.writeValueAsString(methodToVisConfigRequest), ValidateVisualizationTypeConfigurationResponse.class);

                if(!methodToVisConfigValid.isConfigurationValid()){
                    indicatorResponse.setIndicatorSaved(false);
                    indicatorResponse.setErrorMessage("Method to Visualization Port-Configuration is not valid: " + methodToVisConfigValid.getValidationMessage());
                    indicatorResponses.add(indicatorResponse);
                    continue;
                }

                //Saving the triad
                Triad triad = new Triad((AnalyticsGoal) saveRequest.getGoal(), indicatorReference, methodReference, visualizerReference, queryToMethodReference, indicatorRequest.getMethodToVisualizationConfig());
                triad.setCreatedBy(indicatorRequest.getCreatedBy());
                triad.setParameters(indicatorRequest.getParameters());
                String triadJSON = triad.toString();

                Triad savedTriad = performJSONPostRequest(baseUrl + "/AnalyticsModules/Triads/", triadJSON, Triad.class);

                triads.add(savedTriad);
                em.getTransaction().begin();
                em.persist(triads);

                setQuestionTriadMapping(savedQuestion.getId(), savedTriad.getId());

                indicatorResponse.setIndicatorSaved(true);
                indicatorResponse.setIndicatorRequestCode(getIndicatorRequestCode(savedTriad));

                indicatorResponses.add(indicatorResponse);
                em.persist(indicatorResponses);
            }
            catch (Exception exc) {
                indicatorResponse.setIndicatorSaved(false);
                indicatorResponse.setErrorMessage(exc.getMessage());
            }
        }

        questionSaveResponse.setIndicatorSaveResponses(indicatorResponses);
        questionSaveResponse.setQuestionRequestCode(getQuestionRequestCode(triads));
        questionSaveResponse.setQuestionSaved(true);
        em.persist(questionSaveResponse);
        em.flush();
        em.getTransaction().commit();

        return questionSaveResponse;
    }

    private String getIndicatorRequestCode(Triad triad) {
        String visFrameworkScript = "";
        String visualizationURL= "http://localhost:8090/visualizer";
        try{
            visFrameworkScript = performGetRequest(visualizationURL + "/frameworks/" + triad.getVisualizationReference().getLibraryId() + "/methods/"+ triad.getVisualizationReference().getTypeId() + "/frameworkScript");
            visFrameworkScript = decodeURIComponent(visFrameworkScript);
        } catch (Exception exc) {
            throw new ItemNotFoundException(exc.getMessage(),"1");
        }

        String indicatorRequestCode = "<table id='wait_"+triad.getId()+"' style='width: 96%;height: 96%;text-align: center;'><tr><td><img style='-webkit-user-select: none' src='https://www.microsoft.com/about/corporatecitizenship/en-us/youthspark/computerscience/teals/images/loading.gif'> " +
                " <br>Please wait the indicator is being processed</td></tr></table> " +
                visFrameworkScript +
                "<script type=\"text/javascript\"> " +
                "$(document).ready(function() {var xmlhttp_"+triad.getId()+"; " +
                "  if (window.XMLHttpRequest) { xmlhttp_"+triad.getId()+" = new XMLHttpRequest(); } else { xmlhttp_"+triad.getId()+" = new ActiveXObject('Microsoft.XMLHTTP'); } " +
                "  xmlhttp_"+triad.getId()+".onreadystatechange = function (xmlhttp_"+triad.getId()+") {  " +
                "   if (xmlhttp_"+triad.getId()+".currentTarget.readyState == 4) {  " +
                "    $('#wait_"+triad.getId()+"').hide(); " +
                "    $('#main_"+triad.getId()+"').parent().parent().css('overflow','hidden'); " +
                "    $('#main_"+triad.getId()+"').show(); " +
                "    var decResult_"+triad.getId()+" = decodeURIComponent(xmlhttp_"+triad.getId()+".currentTarget.responseText); " +
                "    $('#td_"+triad.getId()+"').append(decResult_"+triad.getId()+"); " +
                "   } " +
                "  }; " +
                "  xmlhttp_"+triad.getId()+".open('GET', '" + indicatorExecutionURL + "?tid="+triad.getId()+"&rid=xxxridxxx&width='+$('#main_"+triad.getId()+"').parent().width()+'&height='+$('#main_"+triad.getId()+"').parent().height(), true);  " +
                "  xmlhttp_"+triad.getId()+".timeout = 300000; " +
                "  xmlhttp_"+triad.getId()+".send(); });" +
                "</script> " +
                "<table id='main_"+triad.getId()+"'><tbody><tr> " +
                " <td id='td_"+triad.getId()+"' style='text-align:-webkit-center;text-align:-moz-center;text-align:-o-center;text-align:-ms-center;'></td></tr></tbody></table> ";

        return  indicatorRequestCode;
    }

    private String getQuestionRequestCode(Set<Triad> triads){

        StringBuilder requestCode = new StringBuilder();

        if(triads.size()>0) {
            requestCode.append("<table style='width:100%;height:100%'>");
            int count = 1;

            for (Triad triad : triads) {

                if(count%2 == 1) requestCode.append("<tr class='questionRows'>");

                requestCode.append("<td>");
                requestCode.append(getIndicatorRequestCode(triad));
                requestCode.append("</td>");

                if(count%2 == 0) requestCode.append("</tr>");

                count++;
            }

            //adding  the closing tr if the number of triads are odd
            if(count%2 == 0) requestCode.append("</tr>");

            requestCode.append("</table>");
        }
        else{
            requestCode.append("No indicators assoicated with the question.");
        }


        return requestCode.toString();
    }

    public OpenLAPDataSet transformIndicatorQueryToOpenLAPDatSet(List<?> dataList, OpenLAPPortConfig methodMapping) {
        OpenLAPDataSet dataset = new OpenLAPDataSet();

        boolean containEntity = false;

        // Creating columns based on the query to method configuration
        try {
            for (OpenLAPColumnConfigData column : methodMapping.getOutputColumnConfigurationData()) {
                if(!dataset.getColumns().containsKey(column.getId()))
                    dataset.addOpenLAPDataColumn(OpenLAPDataColumnFactory.createOpenLAPDataColumnOfType(column.getId(), column.getType(), false, column.getTitle(), column.getDescription()));

                //checking if any mapping have false which means a column needs data from Entity table
                if(!column.isRequired())
                    containEntity = true;
            }
        } catch (OpenLAPDataColumnException e) {
            e.printStackTrace();
        }
        return dataset;
    }

    public List<OpenLAPColumnConfigData> getAnalyticsMethodInputs(String id, HttpServletRequest request) {
        String baseUrl = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());
        List<OpenLAPColumnConfigData> methodInputs = null;

        try {
            ObjectMapper mapper = new ObjectMapper();

            String methodInputsJSON = performGetRequest(baseUrl + "/AnalyticsMethods/"+id+"/getInputPorts");
            methodInputs = mapper.readValue(methodInputsJSON, mapper.getTypeFactory().constructCollectionType(List.class, OpenLAPColumnConfigData.class));
        } catch (Exception exc) {
            System.out.println(exc.getMessage());
            return null;
        }

        return methodInputs;
    }

    public List<OpenLAPColumnConfigData> getAnalyticsMethodOutputs(String id, HttpServletRequest request) {
        String baseUrl = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());
        List<OpenLAPColumnConfigData> methodInputs = null;

        try {
            ObjectMapper mapper = new ObjectMapper();

            String methodInputsJSON = performGetRequest(baseUrl + "/AnalyticsMethods/"+id+"/getOutputPorts");
            methodInputs = mapper.readValue(methodInputsJSON, mapper.getTypeFactory().constructCollectionType(List.class, OpenLAPColumnConfigData.class));
        } catch (Exception exc) {
            System.out.println(exc.getMessage());
            return null;
        }

        return methodInputs;
    }

    public List<OpenLAPDynamicParam> getAnalyticsMethodDynamicParams(String id, HttpServletRequest request) {
        String baseUrl = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());
        List<OpenLAPDynamicParam> methodParams = null;

        try {
            ObjectMapper mapper = new ObjectMapper();

            String methodInputsJSON = performGetRequest(baseUrl + "/AnalyticsMethods/"+id+"/getDynamicParams");
            methodParams = mapper.readValue(methodInputsJSON, mapper.getTypeFactory().constructCollectionType(List.class, OpenLAPDynamicParam.class));
            Collections.sort(methodParams, (OpenLAPDynamicParam o1, OpenLAPDynamicParam o2) -> (o1.getTitle().compareTo(o2.getTitle())));
        } catch (Exception exc) {
            System.out.println(exc.getMessage());
            return null;
        }

        return methodParams;
    }

    public List<OpenLAPColumnConfigData> getVisualizationMethodInputs(String frameworkId, String methodId, HttpServletRequest request) {

        String baseUrl = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());
        List<OpenLAPColumnConfigData> methodInputs = null;

        try {
            ObjectMapper mapper = new ObjectMapper();


            String methodInputsJSON = performGetRequest(baseUrl + "/frameworks/"+frameworkId+"/methods/"+methodId+"/configuration");
            VisualizationTypeConfigurationResponse visResponse = mapper.readValue(methodInputsJSON, VisualizationTypeConfigurationResponse.class);

            methodInputs = visResponse.getTypeConfiguration().getInput().getColumnsConfigurationData();

            Collections.sort(methodInputs, (OpenLAPColumnConfigData o1, OpenLAPColumnConfigData o2) -> (o1.getTitle().compareTo(o2.getTitle())));
        } catch (Exception exc) {
            System.out.println(exc.getMessage());
            return null;
        }

        return methodInputs;
    }

    public IndicatorSaveResponse getIndicatorRequestCode(String triadId, HttpServletRequest request) {

        String baseUrl = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());
        ObjectMapper mapper = new ObjectMapper();

        Triad triad = null;
        IndicatorSaveResponse indicatorResponse = new IndicatorSaveResponse();

        try {
            String triadJSON = performGetRequest(baseUrl + "/AnalyticsModules/Triads/" + triadId);
            triad = mapper.readValue(triadJSON, Triad.class);
        } catch (Exception exc) {
            indicatorResponse.setIndicatorSaved(false);
            indicatorResponse.setErrorMessage("Indicator with triad id '" + triadId + "' not found.");
            return indicatorResponse;
        }

        if (triad == null) {
            indicatorResponse.setIndicatorSaved(false);
            indicatorResponse.setErrorMessage("Indicator with triad id '" + triadId + "' not found.");
        }
        else {
            indicatorResponse.setIndicatorSaved(true);
            indicatorResponse.setIndicatorRequestCode(getIndicatorRequestCode(triad));
            indicatorResponse.setErrorMessage(triad.getIndicatorReference().getIndicators().get("0").getIndicatorName());
        }

        return indicatorResponse;
    }

    public QuestionSaveResponse getQuestionRequestCode(String questionId, HttpServletRequest request) {

        QuestionSaveResponse questionSaveResponse = new QuestionSaveResponse();
        Question question = getQuestionById(questionId);

        if (question == null) {
            questionSaveResponse.setQuestionSaved(false);
            questionSaveResponse.setErrorMessage("Question with id '" + questionId + "' not found.");
            return questionSaveResponse;
        }

        List<IndicatorSaveResponse> indicatorSaveResponses = new ArrayList<IndicatorSaveResponse>();

        for (Triad triad : question.getTriads()) {
            IndicatorSaveResponse indicatorSaveResponse = new IndicatorSaveResponse();

            indicatorSaveResponse.setIndicatorSaved(true);
            indicatorSaveResponse.setIndicatorRequestCode(getIndicatorRequestCode(triad));
            indicatorSaveResponse.setErrorMessage(triad.getIndicatorReference().getIndicators().get("0").getIndicatorName());

            indicatorSaveResponses.add(indicatorSaveResponse);
            em.getTransaction().begin();
            em.persist(indicatorSaveResponse);
            em.flush();
            em.getTransaction().commit();
        }

        questionSaveResponse.setIndicatorSaveResponses(indicatorSaveResponses);
        questionSaveResponse.setQuestionRequestCode(getQuestionRequestCode(question.getTriads()));
        questionSaveResponse.setQuestionSaved(true);
        questionSaveResponse.setErrorMessage(question.getName());

        em.getTransaction().begin();
        em.persist(questionSaveResponse);
        em.flush();
        em.getTransaction().commit();

        return questionSaveResponse;
    }


    public OpenLapUser UserRegistration(OpenLapUser user){

        OpenLapUser openLapUser = new OpenLapUser();
        openLapUser.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        openLapUser.setEmail(user.getEmail());
        openLapUser.setConfirmpassword(bCryptPasswordEncoder.encode(user.getConfirmpassword()));
        openLapUser.setFirstname(user.getFirstname());
        openLapUser.setLastname(user.getLastname());
        em.getTransaction().begin();
        em.persist(openLapUser);
        em.flush();
        em.getTransaction().commit();
        return openLapUser;
    }

/*    public UserDetails loadUserByUsername(String email) {
        // TODO Auto-generated method stub
        OpenLapUser user = em.find(OpenLapUser.class, email);
        if (user == null) {
            logger.error("Invalid username or password.");
            throw new UsernameNotFoundException("Invalid username or password.");
        }
        return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(),
                getAuthority(user.getRoles()));
    }
    private Collection<? extends GrantedAuthority> getAuthority(Collection<Roles> roles){
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());
    }*/
    //HTTP Section

    public String performGetRequest(String url) throws Exception {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        int responseCode = con.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            return response.toString();
        } else {
            throw new Exception(con.getResponseMessage());
        }
    }

    public <T> T performJSONPostRequest(String url, String jsonContent, Class<T> type) throws Exception{
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Accept","application/json;charset=utf-8");

        HttpEntity<String> entity = new HttpEntity<String>(jsonContent,headers);

        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));

        T result = restTemplate.postForObject(url, entity, type);

        return result;
    }

    public String performPutRequest(String url, String jsonContent) throws Exception {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("PUT");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("Content-Length", "" + Integer.toString(jsonContent.getBytes().length));

        con.setUseCaches(false);
        con.setDoInput(true);
        con.setDoOutput(true);

        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(jsonContent);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            return response.toString();
        } else {
            throw new Exception(con.getResponseMessage());
        }
    }

    public String decodeURIComponent(String s) {
        if (s == null) {
            return null;
        }

        String result = null;

        try {
            result = URLDecoder.decode(s, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            result = s;
        }

        return result;
    }

    public String encodeURIComponent(String s) {
        String result = null;

        try {
            result = URLEncoder.encode(s, "UTF-8")
                    .replaceAll("\\%28", "(")
                    .replaceAll("\\%29", ")")
                    .replaceAll("\\+", "%20")
                    .replaceAll("\\%27", "'")
                    .replaceAll("\\%21", "!")
                    .replaceAll("\\%7E", "~");
        } catch (UnsupportedEncodingException e) {
            result = s;
        }
        return result;
    }

}
