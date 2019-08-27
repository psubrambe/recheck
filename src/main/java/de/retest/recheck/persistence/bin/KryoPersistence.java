package de.retest.recheck.persistence.bin;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.objenesis.strategy.StdInstantiatorStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.TreeMultiset;

import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;
import de.retest.recheck.persistence.IncompatibleReportVersionException;
import de.retest.recheck.persistence.Persistable;
import de.retest.recheck.persistence.Persistence;
import de.retest.recheck.util.FileUtil;
import de.retest.recheck.util.VersionProvider;

public class KryoPersistence<T extends Persistable> implements Persistence<T> {

	private static final String OLD_RECHECK_VERSION = "an old recheck version (pre 1.5.0)";

	private static final Logger logger = LoggerFactory.getLogger( KryoPersistence.class );

	private final Kryo kryo;
	private final String version;

	public KryoPersistence() {
		this( createKryo(), VersionProvider.RETEST_VERSION );
	}

	/**
	 * Testing only!
	 *
	 * @param kryo
	 *            {@code Kryo} instance to use.
	 * @param version
	 *            recheck version to use.
	 */
	KryoPersistence( final Kryo kryo, final String version ) {
		this.kryo = kryo;
		this.version = version;
	}

	private static Kryo createKryo() {
		final Kryo kryo = new Kryo();

		kryo.setInstantiatorStrategy( new Kryo.DefaultInstantiatorStrategy( new StdInstantiatorStrategy() ) );

		final Registration registration = kryo.getRegistration( TreeMultiset.class );
		registration.setInstantiator( TreeMultiset::create );

		UnmodifiableCollectionsSerializer.registerSerializers( kryo );

		return kryo;
	}

	@Override
	public void save( final URI identifier, final T element ) throws IOException {
		final Path path = Paths.get( identifier );
		FileUtil.ensureFolder( path.toFile() );
		try ( final Output output = new Output( Files.newOutputStream( path ) ) ) {
			output.writeString( version );
			logger.debug( "Writing {} to {}. Do not write to same identifier or interrupt until done.", element,
					identifier );
			kryo.writeClassAndObject( output, element );
			logger.debug( "Done writing {} to {}", element, identifier );
		}
	}

	private final String VERSION_CHARS = "[\\w\\.\\{\\}\\$]+";

	@SuppressWarnings( "unchecked" )
	@Override
	public T load( final URI identifier ) throws IOException {
		String writerVersion = OLD_RECHECK_VERSION;
		try ( final Input input = new Input( Files.newInputStream( Paths.get( identifier ) ) ) ) {
			writerVersion = input.readString();
			return (T) kryo.readClassAndObject( input );
		} catch ( final KryoException e ) {
			if ( version.equals( writerVersion ) ) {
				throw e;
			}
			if ( writerVersion == null || !writerVersion.matches( VERSION_CHARS ) ) {
				writerVersion = OLD_RECHECK_VERSION;
			}
			throw new IncompatibleReportVersionException( writerVersion, version, identifier, e );
		}
	}

}
