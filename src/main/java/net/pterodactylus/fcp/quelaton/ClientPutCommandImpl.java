package net.pterodactylus.fcp.quelaton;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import net.pterodactylus.fcp.ClientPut;
import net.pterodactylus.fcp.FcpMessage;
import net.pterodactylus.fcp.Key;
import net.pterodactylus.fcp.PutFailed;
import net.pterodactylus.fcp.PutSuccessful;
import net.pterodactylus.fcp.UploadFrom;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * Default {@link ClientPutCommand} implemented based on {@link FcpReplySequence}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
class ClientPutCommandImpl implements ClientPutCommand {

	private final ListeningExecutorService threadPool;
	private final ConnectionSupplier connectionSupplier;
	private final AtomicReference<String> redirectUri = new AtomicReference<>();
	private final AtomicReference<File> file = new AtomicReference<>();
	private final AtomicReference<InputStream> payload = new AtomicReference<>();
	private final AtomicLong length = new AtomicLong();
	private final AtomicReference<String> targetFilename = new AtomicReference<>();

	public ClientPutCommandImpl(ExecutorService threadPool, ConnectionSupplier connectionSupplier) {
		this.threadPool = MoreExecutors.listeningDecorator(threadPool);
		this.connectionSupplier = connectionSupplier;
	}

	@Override
	public ClientPutCommand named(String targetFilename) {
		this.targetFilename.set(targetFilename);
		return this;
	}

	@Override
	public Keyed<Optional<Key>> redirectTo(Key key) {
		this.redirectUri.set(Objects.requireNonNull(key, "key must not be null").getKey());
		return this::key;
	}

	@Override
	public Keyed<Optional<Key>> from(File file) {
		this.file.set(Objects.requireNonNull(file, "file must not be null"));
		return this::key;
	}

	@Override
	public Lengthed<Keyed<Optional<Key>>> from(InputStream inputStream) {
		payload.set(Objects.requireNonNull(inputStream, "inputStream must not be null"));
		return this::length;
	}

	private Keyed<Optional<Key>> length(long length) {
		this.length.set(length);
		return this::key;
	}

	private ListenableFuture<Optional<Key>> key(Key key) {
		String identifier = new RandomIdentifierGenerator().generate();
		ClientPut clientPut = createClientPutCommand(key.getKey(), identifier);
		return threadPool.submit(() -> new ClientPutReplySequence().send(clientPut).get());
	}

	private ClientPut createClientPutCommand(String uri, String identifier) {
		ClientPut clientPut;
		if (file.get() != null) {
			clientPut = createClientPutFromDisk(uri, identifier, file.get());
		} else if (redirectUri.get() != null) {
			clientPut = createClientPutRedirect(uri, identifier, redirectUri.get());
		} else {
			clientPut = createClientPutDirect(uri, identifier, length.get(), payload.get());
		}
		if (targetFilename.get() != null) {
			clientPut.setTargetFilename(targetFilename.get());
		}
		return clientPut;
	}

	private ClientPut createClientPutFromDisk(String uri, String identifier, File file) {
		ClientPut clientPut = new ClientPut(uri, identifier, UploadFrom.disk);
		clientPut.setFilename(file.getAbsolutePath());
		return clientPut;
	}

	private ClientPut createClientPutRedirect(String uri, String identifier, String redirectUri) {
		ClientPut clientPut = new ClientPut(uri, identifier, UploadFrom.redirect);
		clientPut.setTargetURI(redirectUri);
		return clientPut;
	}

	private ClientPut createClientPutDirect(String uri, String identifier, long length, InputStream payload) {
		ClientPut clientPut = new ClientPut(uri, identifier, UploadFrom.direct);
		clientPut.setDataLength(length);
		clientPut.setPayloadInputStream(payload);
		return clientPut;
	}

	private class ClientPutReplySequence extends FcpReplySequence<Optional<Key>> {

		private final AtomicReference<String> identifier = new AtomicReference<>();
		private final AtomicReference<Key> finalKey = new AtomicReference<>();
		private final AtomicBoolean putFinished = new AtomicBoolean();

		public ClientPutReplySequence() throws IOException {
			super(ClientPutCommandImpl.this.threadPool, ClientPutCommandImpl.this.connectionSupplier.get());
		}

		@Override
		protected boolean isFinished() {
			return putFinished.get();
		}

		@Override
		protected Optional<Key> getResult() {
			return Optional.ofNullable(finalKey.get());
		}

		@Override
		public ListenableFuture<Optional<Key>> send(FcpMessage fcpMessage) throws IOException {
			identifier.set(fcpMessage.getField("Identifier"));
			return super.send(fcpMessage);
		}

		@Override
		protected void consumePutSuccessful(PutSuccessful putSuccessful) {
			if (putSuccessful.getIdentifier().equals(identifier.get())) {
				finalKey.set(new Key(putSuccessful.getURI()));
				putFinished.set(true);
			}
		}

		@Override
		protected void consumePutFailed(PutFailed putFailed) {
			if (putFailed.getIdentifier().equals(identifier.get())) {
				putFinished.set(true);
			}
		}

		@Override
		protected void consumeConnectionClosed(Throwable throwable) {
			putFinished.set(true);
		}
	}

}