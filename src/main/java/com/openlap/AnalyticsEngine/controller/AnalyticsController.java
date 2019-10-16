package com.openlap.AnalyticsEngine.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.openlap.AnalyticsEngine.dto.LoginUser;
import com.openlap.AnalyticsEngine.dto.Request.IndicatorPreviewRequest;
import com.openlap.AnalyticsEngine.dto.Request.QuestionSaveRequest;
import com.openlap.AnalyticsEngine.dto.Response.*;
import com.openlap.AnalyticsEngine.model.OpenLapUser;
import com.openlap.AnalyticsEngine.service.AnalyticsEngineService;
import com.openlap.AnalyticsMethods.model.AnalyticsMethods;
import com.openlap.AnalyticsModules.model.AnalyticsGoal;
import com.openlap.Visualizer.dtos.response.VisualizationLibraryDetailsResponse;
import com.openlap.Visualizer.dtos.response.VisualizationLibrariesDetailsResponse;

import de.rwthaachen.openlap.dataset.OpenLAPColumnConfigData;
import de.rwthaachen.openlap.dynamicparam.OpenLAPDynamicParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/AnalyticsEngine/")
public class AnalyticsController {

    @Autowired
    AnalyticsEngineService analyticsEngineService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @RequestMapping(value = {"/GetGoals/", "/GetAllGoals"}, method = RequestMethod.GET)
    public
    @ResponseBody
    List<AnalyticsGoal> GetAllGoals(@RequestParam Map<String, String> allRequestParams, HttpServletRequest request) {
        return analyticsEngineService.getAllGoals(request);
    }

    @RequestMapping(value = {"/GetGoals/", "/GetActiveGoals"}, method = RequestMethod.GET)
    public
    @ResponseBody
    List<AnalyticsGoal> GetActiveGoals(@RequestParam(value = "uid", required = false) String uid,
                                       @RequestParam Map<String, String> allRequestParams,
                                       HttpServletRequest request) {
        return analyticsEngineService.getActiveGoals(uid, request);
    }

    @RequestMapping(value = {"/SaveGoal/", "/SaveGoal"}, method = RequestMethod.GET)
    public
    @ResponseBody
    AnalyticsGoal SaveGoal(@RequestParam String name,
                           @RequestParam String description,
                           @RequestParam String author,
                           @RequestParam Map<String, String> allRequestParams, HttpServletRequest request) {
        return analyticsEngineService.saveGoal(name, description, author, request);
    }

    @RequestMapping(value = {"/SetGoalStatus/", "/SetGoalStatus"}, method = RequestMethod.GET)
    public
    @ResponseBody
    AnalyticsGoal SetGoalStatus(@RequestParam String goalId,
                                @RequestParam boolean isActive,
                                @RequestParam Map<String, String> allRequestParams, HttpServletRequest request) {
        return analyticsEngineService.setGoalStatus(goalId, isActive, request);
    }

    @RequestMapping(value = {"/GetAnalyticsMethods/", "/GetAnalyticsMethods"}, method = RequestMethod.GET)
    public
    @ResponseBody
    List<AnalyticsMethods> GetAllMethods(@RequestParam Map<String, String> allRequestParams, HttpServletRequest request) {
        return analyticsEngineService.getAllAnalyticsMethods(request);
    }

    @RequestMapping(value = {"/GetVisualizations/", "/GetVisualizations"}, method = RequestMethod.GET)
    public
    @ResponseBody
    VisualizationLibrariesDetailsResponse GetAllVisualizations(@RequestParam Map<String, String> allRequestParams, HttpServletRequest request) {
        return analyticsEngineService.getAllVisualizations(request);
    }

    @RequestMapping(value = {"/GetVisualizationMethods/", "/GetVisualizationMethods"}, method = RequestMethod.GET)
    public
    @ResponseBody
    VisualizationLibraryDetailsResponse GetVisualizationMethods(@RequestParam String frameworkId, @RequestParam Map<String, String> allRequestParams, HttpServletRequest request) {
        return analyticsEngineService.getVisualizationsMethods(frameworkId, request);
    }

    @RequestMapping(value = {"/GetQuestions/", "/GetQuestions"}, method = RequestMethod.GET)
    public
    @ResponseBody
    List<QuestionResponse> GetQuestions(@RequestParam Map<String, String> allRequestParams, HttpServletRequest request) {
        return analyticsEngineService.getQuestions(request);
    }

    @RequestMapping(value = {"/ValidateQuestionName/", "/ValidateQuestionName"}, method = RequestMethod.GET)
    public
    @ResponseBody
    Boolean ValidateQuestionName(@RequestParam String name) {
        return analyticsEngineService.validateQuestionName(name);
    }

    @RequestMapping(value = {"/GetIndicators/", "/GetIndicators"}, method = RequestMethod.GET)
    public
    @ResponseBody
    List<IndicatorResponse> GetIndicators(@RequestParam Map<String, String> allRequestParams, HttpServletRequest request) {
        return analyticsEngineService.getIndicators(request);
    }

    @RequestMapping(value = {"/GetIndicatorsByQuestionId/", "/GetIndicatorsByQuestionId"}, method = RequestMethod.GET)
    public
    @ResponseBody
    List<IndicatorResponse> GetIndicatorsByQuestionId(@RequestParam String questionId, @RequestParam Map<String, String> allRequestParams, HttpServletRequest request) {
        return analyticsEngineService.getIndicatorsByQuestionId(questionId, request);
    }

    @RequestMapping(value = {"/ValidateIndicatorName/", "/ValidateIndicatorName"}, method = RequestMethod.GET)
    public
    @ResponseBody
    Boolean ValidateIndicatorName(@RequestParam String name) {
        return analyticsEngineService.validateIndicatorName(name);
    }

    @RequestMapping(value = {"/GetIndicatorData/", "/GetIndicatorData"}, method = RequestMethod.GET)
    public
    @ResponseBody
    String GetIndicatorData(
            @RequestParam(value = "tid", required = true) String triadID,
            @RequestParam Map<String, String> allRequestParams,
            HttpServletRequest request) {

        //String baseUrl = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());
        String baseUrl = request.getRequestURL().toString().replace(request.getServletPath(),"");

        try {
            //return analyticsEngineService.executeIndicator(allRequestParams, baseUrl);
            return "Outdated request call. Please get new indicator request code from OpenLAP.";
        } catch (Exception exc) {
            return exc.getMessage();
        }
    }

    @RequestMapping(value = {"/GetIndicatorById/", "/GetIndicatorById"}, method = RequestMethod.GET)
    public
    @ResponseBody
    IndicatorResponse GetTriadById(@RequestParam String indicatorId, @RequestParam Map<String, String> allRequestParams, HttpServletRequest request) {
        return analyticsEngineService.getTriadById(indicatorId, request);
    }


    @RequestMapping(value = "/SaveQuestionAndIndicators", method = RequestMethod.POST)
    public
    @ResponseBody
    QuestionSaveResponse SaveQuestionAndIndicators(
            @RequestBody QuestionSaveRequest questionSaveRequest,
            HttpServletRequest request) {

        //String baseUrl = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());
        String baseUrl = request.getRequestURL().toString().replace(request.getServletPath(),"");

        return analyticsEngineService.saveQuestionAndIndicators(questionSaveRequest, request);
    }
    @RequestMapping(value = "/GetIndicatorPreview", method = RequestMethod.POST)
    public
    @ResponseBody
    IndicatorPreviewResponse GetIndicatorPreview(
            @RequestBody IndicatorPreviewRequest indicatorPreviewRequest,
            @RequestParam Map<String, String> allRequestParams,
            HttpServletRequest request) throws JsonProcessingException {

        //String baseUrl = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());
        String baseUrl = request.getRequestURL().toString().replace(request.getServletPath(),"");
        return analyticsEngineService.getIndicatorPreview(indicatorPreviewRequest, baseUrl);
    }

    @RequestMapping(value = "/UserRegistration", method = RequestMethod.POST)
    public
    @ResponseBody
    OpenLapUser UserRegistration(@RequestBody OpenLapUser openLapUser){
        return analyticsEngineService.UserRegistration(openLapUser);
    }

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public
    @ResponseBody
    void login(@RequestBody OpenLapUser openLapUser) throws AuthenticationException {

        final Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(openLapUser.getEmail(), openLapUser.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @RequestMapping(value = "/GetCompositeIndicatorPreview", method = RequestMethod.POST)
    public
    @ResponseBody
    IndicatorPreviewResponse GetCompositeIndicatorPreview(
            @RequestBody IndicatorPreviewRequest previewRequest,
            @RequestParam Map<String, String> allRequestParams,
            HttpServletRequest request) {

        String baseUrl = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());

        return analyticsEngineService.getCompIndicatorPreview(previewRequest, baseUrl);
    }

    @RequestMapping(value = "/GetMLAIIndicatorPreview", method = RequestMethod.POST)
    public
    @ResponseBody
    IndicatorPreviewResponse GetMLAIIndicatorPreview(
            @RequestBody IndicatorPreviewRequest previewRequest,
            @RequestParam Map<String, String> allRequestParams,
            HttpServletRequest request) {

        String baseUrl = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());

        return analyticsEngineService.getMLAIIndicatorPreview(previewRequest, baseUrl);
    }

    @RequestMapping(value = {"/GetAnalyticsMethodInputs/", "/GetAnalyticsMethodInputs"}, method = RequestMethod.GET)
    public
    @ResponseBody
    List<OpenLAPColumnConfigData> GetAnalyticsMethodInputs(@RequestParam String id, @RequestParam Map<String, String> allRequestParams, HttpServletRequest request) {
        return analyticsEngineService.getAnalyticsMethodInputs(id, request);
    }

    @RequestMapping(value = {"/GetAnalyticsMethodOutputs/", "/GetAnalyticsMethodOutputs"}, method = RequestMethod.GET)
    public
    @ResponseBody
    List<OpenLAPColumnConfigData> GetAnalyticsMethodOutputs(@RequestParam String id, @RequestParam Map<String, String> allRequestParams, HttpServletRequest request) {
        return analyticsEngineService.getAnalyticsMethodOutputs(id, request);
    }

    @RequestMapping(value = {"/GetAnalyticsMethodParams/", "/GetAnalyticsMethodParams"}, method = RequestMethod.GET)
    public
    @ResponseBody
    List<OpenLAPDynamicParam> GetAnalyticsMethodParams(@RequestParam String id, @RequestParam Map<String, String> allRequestParams, HttpServletRequest request) {
        return analyticsEngineService.getAnalyticsMethodDynamicParams(id, request);
    }

    @RequestMapping(value = {"/GetVisualizationMethodInputs/", "/GetVisualizationMethodInputs"}, method = RequestMethod.GET)
    public
    @ResponseBody
    List<OpenLAPColumnConfigData> GetVisualizationMethodInputs(@RequestParam String frameworkId, @RequestParam long methodId, @RequestParam Map<String, String> allRequestParams, HttpServletRequest request) {
        return analyticsEngineService.getVisualizationMethodInputs(frameworkId, methodId, request);
    }

    @RequestMapping(value = {"/GetIndicatorRequestCode/", "/GetIndicatorRequestCode"}, method = RequestMethod.GET)
    public
    @ResponseBody
    IndicatorSaveResponse GetIndicatorRequestCode(@RequestParam String indicatorId, HttpServletRequest request) {
        return analyticsEngineService.getIndicatorRequestCode(indicatorId, request);
    }

    @RequestMapping(value = {"/GetQuestionRequestCode/", "/GetQuestionRequestCode"}, method = RequestMethod.GET)
    public
    @ResponseBody
    QuestionSaveResponse GetQuestionRequestCode(@RequestParam String questionId, HttpServletRequest request) {
        return analyticsEngineService.getQuestionRequestCode(questionId, request);
    }

}