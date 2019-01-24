package de.retest.persistence.xml;

import java.io.IOException;

import javax.xml.bind.Unmarshaller.Listener;

import de.retest.persistence.Persistable;
import de.retest.persistence.migration.XmlMigrator;
import de.retest.persistence.xml.util.XmlVersionCheckResult;
import de.retest.util.NamedBufferedInputStream;

public class XmlPersistenceUtil {

	static <T extends Persistable> ReTestXmlDataContainer<T> migrateAndRead( final XmlTransformer xml,
			final NamedBufferedInputStream inputStream, final Listener unmarshallListener ) throws IOException {
		NamedBufferedInputStream bin = inputStream;

		final XmlVersionCheckResult checkResult = XmlVersionCheckResult.create( bin );

		if ( checkResult.newDataTypeInstance == null ) {
			throw new RuntimeException( "Unexpected data type " + checkResult.oldDataType );
		}

		if ( !checkResult.isCompatible() ) {
			bin = XmlMigrator.tryToMigrate( checkResult, bin );
			if ( bin == null ) {
				throw new RuntimeException( "Could not migrate XML." );
			}
		}

		@SuppressWarnings( "unchecked" )
		final ReTestXmlDataContainer<T> result = (ReTestXmlDataContainer<T>) xml.fromXML( bin, unmarshallListener );
		return result;
	}

}
