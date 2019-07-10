package de.retest.recheck;

import java.io.IOException;
import java.util.prefs.Preferences;

import org.keycloak.adapters.ServerRequest.HttpFailure;
import org.keycloak.common.VerificationException;

import de.retest.recheck.Properties.FileOutputFormat;
import de.retest.recheck.auth.RehubAuthenticationHandler;
import de.retest.recheck.auth.RetestAuthentication;
import de.retest.recheck.persistence.CloudPersistence;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Rehub {

	private Rehub() {

	}

	public static void init() {
		System.setProperty( RetestAuthentication.RESOURCE_PROPERTY, "marvin" );

		final RetestAuthentication auth = RetestAuthentication.getInstance();

		try {
			if ( !auth.isAuthenticated( getToken() ) ) {
				auth.login( new RehubAuthenticationHandler() );
			}
			System.setProperty( Properties.FILE_OUTPUT_FORMAT_PROPERTY, FileOutputFormat.CLOUD.toString() );
		} catch ( IOException | HttpFailure | VerificationException e ) {
			log.error( "Error verifying offline token", e );
		}
	}

	private static String getToken() {
		final String tokenFromEnvironment = System.getenv( CloudPersistence.RECHECK_API_KEY );
		final String tokenFromPreferences =
				Preferences.userNodeForPackage( Rehub.class ).get( CloudPersistence.RECHECK_API_KEY, null );

		return tokenFromEnvironment != null ? tokenFromEnvironment : tokenFromPreferences;
	}

}
