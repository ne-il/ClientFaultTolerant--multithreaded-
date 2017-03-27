import java.awt.image.BufferedImageFilter;
import java.io.IOException;
import java.lang.Character.UnicodeScript;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.jar.Pack200.Packer;

public class MyClientBetterUpperCaseUDPFaultTolerant {

	private static final int BUFF_SIZE = 128;

	static void usage() {
		System.out.println("DestinationHost DestinationPort CHARSET");
	}

	static ByteBuffer createPacket(String charsetName, String message) {

		ByteBuffer charsetBuffer = Charset.forName("US-ASCII").encode(charsetName);

		ByteBuffer messageBuffer = Charset.forName(charsetName).encode(message);

		int lengthCharsetName = charsetBuffer.remaining();

		ByteBuffer packetBuffer = ByteBuffer
				.allocate(Integer.BYTES + charsetBuffer.remaining() + messageBuffer.remaining());

		packetBuffer.putInt(lengthCharsetName);
		packetBuffer.put(charsetBuffer);
		packetBuffer.put(messageBuffer);

		packetBuffer.flip();
		return packetBuffer;
	}

	static String readPacket(ByteBuffer packetBuffer) {
		int charsetNameSize = packetBuffer.getInt();

		int oldLimit = packetBuffer.limit();

		packetBuffer.limit(packetBuffer.position() + charsetNameSize);

		CharBuffer charsetName = Charset.forName("US-ASCII").decode(packetBuffer);

		packetBuffer.limit(oldLimit);

		CharBuffer message = Charset.forName(charsetName.toString()).decode(packetBuffer);

		System.out.println("size of Charset Name-> " + charsetNameSize);
		System.out.println("Charset -> " + charsetName);
		System.out.println("Message -> " + message);

		return message.toString();
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 3) {
			usage();
			return;
		}

		String destinationHost = args[0];
		int destinationPort = Integer.parseInt(args[1]);
		String charsetName = args[2];

		InetSocketAddress adressDestination = new InetSocketAddress(destinationHost, destinationPort);
		DatagramChannel socketLocale = DatagramChannel.open().bind(null);

		BlockingQueue<ByteBuffer> bQueue = new ArrayBlockingQueue<>(1);

		Scanner scan = new Scanner(System.in);

		Thread listenerThread = new Thread(() -> {

			ByteBuffer buffToReceive = ByteBuffer.allocate(BUFF_SIZE);

			while (!Thread.currentThread().isInterrupted()) {
				try {
					socketLocale.receive(buffToReceive);
					buffToReceive.flip();

				} catch (Exception e) {
				}
				try {
					bQueue.put(buffToReceive.duplicate());
				} catch (Exception e) {
				}
				buffToReceive.clear();
			}

		});
		listenerThread.setName("ListenerThread");
		listenerThread.start();

		while (scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.isEmpty()) {
				break;
			}

			ByteBuffer headOfQueue = null;
			do {
				headOfQueue = null;
				ByteBuffer bufferTosend = createPacket(charsetName, line);
				socketLocale.send(bufferTosend.duplicate(), adressDestination);
				try {
					headOfQueue = bQueue.poll(1, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (headOfQueue == null) {
					System.out.println("PAS DE REPONSE au bout de 1 seconde on re-envoie\n");

				} else {
					String responseString = readPacket(headOfQueue);

				}

			} while (headOfQueue == null);

		}
		System.out.println("Bye!");
		listenerThread.interrupt();
		socketLocale.close();
		scan.close();
	}

}
