

public class BopEXClient extends AbstractEXClient {
    private static final Logger LOG = Logger.getLogger(BopRestClient.class.toString());
    private static final String APP_KEY = "appKey";
    private static final String FORMAT = "format";
    private static final String METHOD = "method";
    private static final String TIMESTAMP = "timestamp";
    private static final String VERSION = "version";
    private static final String SIGN = "sign";
    private static final String SESSION = "token";
    private static final String TYPE = "type";
    private static final String REQUEST_ID = "requestId";
    private String serverUrl;
    private String appKey;
    private String appSecret;
    private String format = "json";
    private WebUtils webUtils;
    private static ObjectMapper objectMapper = JacksonUtil.getMapper();

    public BopEXClient(String serverUrl, String appKey, String appSecret) {
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.serverUrl = serverUrl;
        this.getWebUtil();
    }

    private WebUtils getWebUtil() {
        if (this.webUtils == null) {
            this.webUtils = new WebUtils();
        }

        return this.webUtils;
    }

    public <T> T execute(IBopRequest request, String requestId, String session, Class<T> respClazz) throws BopException {
        if (StrUtils.isEmpty(requestId)) {
            requestId = UUID.randomUUID().toString();
        }

        String requestJson = JacksonUtil.beanToString(request);
        if (OperationController.canLog(request.getApiName())) {
        }

        String postServerUrl = this.appendSystemParamsToUrl(this.serverUrl, request.getApiName(), session, "sync", (String)null, requestId, requestJson);

        String resp;
        Class rClazz;
        try {
            IEXHandler handler = IEXHandlerFactory.getHandler(request.getApiName());
            rClazz = handler.getResponseClass();
            IBopClient bopClient = new BopRestClient(this.serverUrl, this.appKey, this.appSecret);
            resp = handler.handle(bopClient, request, session, requestId);
        } catch (Exception var11) {
            throw new BopException(var11);
        }

        resp = this.convertResponse(resp, rClazz, requestId, request.getApiName());
        this.tryParseException(resp);
        return this.getResposeParser(this.format, respClazz).parse(resp, request.getApiName());
    }

    private String convertResponse(String resp, Class rClazz, String requestId, String method) {
        if (rClazz != null && resp != null) {
            Result result = null;
            String simpleName = rClazz.getSimpleName();
            if ("BWJsonResult".equals(simpleName)) {
                result = this.convertBWJsonResult(resp, requestId);
            } else if ("BopsResult".equals(simpleName)) {
                result = this.convertBopsResult(resp, requestId);
            }

            if (result != null) {
                resp = JacksonUtil.beanToString(result);
            }

            String version = "6.0";

            ErrorResponse errorResponse;
            try {
                JsonNode responseJson = objectMapper.readTree(resp);
                if (resp.contains("success") && !responseJson.get("success").asBoolean()) {
                    errorResponse = ErrorResponse.newErrorResponse(method, requestId, ParamCheck.checkErrorCode(version, ParamCheck.checkErrorCode(version, 100006, this.appKey), this.appKey), "远程访问错误", responseJson.get("message").get("errorCode").asInt(), responseJson.get("message").get("errorMessage").asText());
                    return JacksonUtil.beanToString(errorResponse);
                }

                if (resp.contains("success") && responseJson.get("success").asBoolean()) {
                    Response response = new Response(method, requestId, responseJson.get("model"));
                    return JacksonUtil.beanToString(response);
                }
            } catch (Exception var10) {
                JacksonUtil.beanToString(errorResponse);
            }

            return resp;
        } else {
            return null;
        }
    }

    private Result convertBopsResult(String resp, String requestId) {
        BopsResult bopsResult = (BopsResult)JacksonUtil.jsonStrToObject(resp, BopsResult.class);
        ResultBuilder builder = ResultBuilder.newResult();
        Boolean success = bopsResult.getSuccess();
        Object data = bopsResult.getData();
        String message = bopsResult.getMessage();
        String code = bopsResult.getCode();
        String errorDetail = null;
        String traceId = null;
        if (null != bopsResult.getTraceInfo()) {
            errorDetail = bopsResult.getTraceInfo().getErrorDetail();
            traceId = bopsResult.getTraceInfo().getTraceId();
        }

        ResultMsg resultMsg = null;
        if (success) {
            if (!StrUtils.isEmpty(message)) {
                resultMsg = new ResultMsg();
                resultMsg.setSuccessMessage(message);
            }
        } else {
            resultMsg = new ResultMsg();
            resultMsg.setInnerRequestId(traceId);
            resultMsg.setErrorCode(code);
            if (null == errorDetail) {
                resultMsg.setErrorMessage(message);
            } else {
                resultMsg.setErrorMessage(errorDetail);
            }
        }

        Result result = builder.setModel(data).setRequestId(requestId).setIsSuccess(success).setResultMsg(resultMsg).build();
        return result;
    }

    private Result convertBWJsonResult(String resp, String requestId) {
        BWJsonResult bwJsonResult = (BWJsonResult)JacksonUtil.jsonStrToObject(resp, BWJsonResult.class);
        if (bwJsonResult == null) {
            return new Result();
        } else {
            Object responseBody = bwJsonResult.getData();
            boolean success = bwJsonResult.isSuccess();
            String message = bwJsonResult.getMessage();
            String errorCode = bwJsonResult.getErrorCode();
            String errorMsg = bwJsonResult.getErrorMsg();
            String innerRequestId = bwJsonResult.getRequestID();
            if (StrUtils.isEmpty(innerRequestId)) {
                innerRequestId = bwJsonResult.getRequestId();
            }

            ResultMsg resultMsg = null;
            if (success) {
                if (!StrUtils.isEmpty(message)) {
                    resultMsg = new ResultMsg();
                    resultMsg.setSuccessMessage(message);
                }
            } else {
                resultMsg = new ResultMsg();
                resultMsg.setInnerRequestId(innerRequestId);
                resultMsg.setErrorCode(errorCode);
                resultMsg.setErrorMessage(StrUtils.isEmpty(message) ? errorMsg : message);
            }

            Result result = ResultBuilder.newResult().setModel(responseBody).setIsSuccess(success).setRequestId(requestId).setResultMsg(resultMsg).build();
            return result;
        }
    }

    private <T> IBopResposeParser<T> getResposeParser(String format, Class<T> clazz) {
        return (IBopResposeParser)(format.equals("xml") ? new XmlResposeParser(clazz) : new JsonResposeParser(clazz));
    }

    private void tryParseException(String rsp) throws BopException {
        if ("json".equals(this.format)) {
            BopUtils.tryParseException(rsp);
        } else {
            BopUtils.tryParseXmlException(rsp);
        }

    }

    private String appendSystemParamsToUrl(String url, String apiName, String session, String type, String asynId, String requestId, String body) {
        Long time = (new Date()).getTime();
        StringBuilder sburl = new StringBuilder(url);
        sburl.append("?");
        sburl.append("method");
        sburl.append("=");
        sburl.append(apiName);
        sburl.append("&");
        sburl.append("version");
        sburl.append("=6.0&");
        sburl.append("appKey");
        sburl.append("=");
        sburl.append(this.appKey);
        sburl.append("&");
        sburl.append("format");
        sburl.append("=");
        sburl.append(this.format);
        sburl.append("&");
        sburl.append("timestamp");
        sburl.append("=");
        sburl.append(time);
        sburl.append("&");
        sburl.append("token");
        sburl.append("=");
        sburl.append(session);
        sburl.append("&");
        sburl.append("type");
        sburl.append("=");
        sburl.append(type);
        if (asynId != null) {
            sburl.append("&");
            sburl.append("asynRequestId");
            sburl.append("=");
            sburl.append(asynId);
        }

        if (requestId != null) {
            sburl.append("&");
            sburl.append("requestId");
            sburl.append("=");
            sburl.append(requestId);
        }

        try {
            BopHashMap textParams = new BopHashMap();
            textParams.put("method", apiName);
            textParams.put("version", "6.0");
            textParams.put("appKey", this.appKey);
            textParams.put("format", this.format);
            textParams.put("timestamp", time);
            textParams.put("token", session);
            textParams.put("type", type);
            if (requestId != null) {
                textParams.put("requestId", requestId);
            }

            if (asynId != null) {
                textParams.put("asynRequestId", asynId);
            }

            sburl.append("&");
            sburl.append("sign");
            sburl.append("=");
            sburl.append(BopUtils.signTopRequest(textParams, this.appSecret, body));
        } catch (Exception var11) {
            throw new BopException(var11);
        }

        return sburl.toString();
    }
}
