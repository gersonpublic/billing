package com.procarchile;

import cl.transbank.webpay.Webpay;
import cl.transbank.webpay.WebpayOneClick;
import cl.transbank.webpay.configuration.Configuration;
import com.transbank.webpayserver.webservices.OneClickFinishInscriptionOutput;
import com.transbank.webpayserver.webservices.OneClickInscriptionOutput;
import org.apache.log4j.BasicConfigurator;
import org.glassfish.hk2.utilities.reflection.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

@Path("tbk")
public class TbkConnection {

    static Logger logger = Logger.getLogger();

    @Path("/inscription")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response tbkInscription(
            @Context HttpServletRequest request,
            @Context HttpServletResponse response,
            @HeaderParam("x-language-code") String lang,
            @HeaderParam("x-api-key") String sessionKeyHeader,
            @QueryParam("userEmail") String userEmail,
            @QueryParam("userName") String username,
            @QueryParam("finish") boolean finish
    ) throws Exception {

        BasicConfigurator.configure();
        Map<String, String> output = new HashMap<String, String>();
        String formAction = "";
        String tbkToken =  "";
        String user = userEmail;
        String userName = username;
        String email = userEmail;


        logger.debug("\n\n\nIniciando inscripción... creando conexion webpayoneclick");

        //Webpay transaction = new Webpay(Configuration.forTestingWebpayOneClickNormal());
        WebpayOneClick transaction = new Webpay( Configuration.forTestingWebpayOneClickNormal() ).getOneClickTransaction();

        if( !finish ) {

            String returnUrl = "https://localhost:8080/api/tbkt/inscription.json?user=" + user + "&finish=true";
            //String returnUrl = request.getRequestURI() + "?user=" + user + "&finish=true";

            OneClickInscriptionOutput initResult = new OneClickInscriptionOutput();

            try {

                logger.debug("\n\nParámetros : \nuser: " +user + "\nuserName: " + userName +"\nurlReturn: " +returnUrl);

                //initResult =  transaction.getOneClickTransaction().initInscription( user, email, returnUrl);
                initResult = transaction.initInscription(user, email, returnUrl);

                tbkToken = initResult.getToken();
                formAction = initResult.getUrlWebpay();

                logger.debug("\n\n\nWebPay url: " + formAction + " Token : " + tbkToken);

               /* URL tbkUrl = new URL(formAction);
                HttpURLConnection connection = (HttpURLConnection) tbkUrl.openConnection();

                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agen", "Mozilla/5.0");
                String parameters = "TBK_TOKEN=" + tbkToken;

                connection.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                wr.writeBytes(parameters);
                wr.flush();
                wr.close();

                int responseCode = connection.getResponseCode();
                logger.info("\nSending 'POST' request to URL : " + formAction);
                logger.info("\nPost parameters : " + parameters);
                logger.info("\nResponse Code : " + responseCode);

                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuffer responsed = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    responsed.append(inputLine);
                }
                in.close();

                //print result
                logger.info("\n\nResult: "+responsed.toString());
                */

                output.put("tbkUrl", formAction );
                output.put("tbkVariable","TBK_TOKEN");
                output.put("tokenValue", tbkToken);

            } catch (Exception e) {
                logger.warning("\n\n\nError en el proceso de inscripcion del usuario" , e);
                output.put("message", e.toString());
                output.put("success","error");
                return sendTbkResponse( output );
            }

            //return Response.seeOther( URI.create( formAction ) ).build();

        }else {

            String tbkUser= "";
            String tbkAuthCode = "";
            String last4Digits = "";
            String creditCardType = "";


            OneClickFinishInscriptionOutput result = new OneClickFinishInscriptionOutput();

            try {
                tbkToken = request.getParameter("TBK_TOKEN" );

                //result =  transaction.getOneClickTransaction().finishInscription( tbkToken );
                result =  transaction.finishInscription( tbkToken );

                if ( result.getResponseCode() == 0 ){
                    logger.debug("Inscripción en transbank exitosa");
                    tbkToken = result.getTbkUser();
                    last4Digits = result.getLast4CardDigits();
                    creditCardType = result.getCreditCardType().value();
                    output.put("succes" ,"ok");
                    output.put("tbkToken", tbkToken);
                    output.put("tbkAuthCode", tbkAuthCode);
                    output.put("last4Digits", last4Digits);
                    output.put("creditCardType", creditCardType );
                } else {
                    logger.debug("\n\n\nCodigo de Respuesta trasnbank para la inscripción : " +  result.getResponseCode() );
                    output.put("response", Integer.toString( result.getResponseCode() ) );
                }

            }catch(Exception e){
                logger.warning("Error al tratar de finalizar la inscripción " , e );
                output.put("fail","Error");
            }
        }
        return sendTbkResponse( output );
    }

    public static Response sendTbkResponse( Object response) {
        return Response.ok( response ).build();
    }
}
