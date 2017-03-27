import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class ClientUpperCaseUDPFaultTolerant {
	private static final int BUFSIZ = 1024;

	public static void main(String[] args) throws IOException {
		String hostName = args[0];
		int portNum = Integer.parseInt(args[1]);

		DatagramChannel socketLocale = DatagramChannel.open().bind(null);
		InetSocketAddress adresseDestination = new InetSocketAddress(hostName, portNum);

		Charset charset = Charset.forName(args[2]);
		Scanner scanner = new Scanner(System.in);

		BlockingQueue<String> blockingQueue = new ArrayBlockingQueue<>(1);

		Thread listenerThread = new Thread(() -> {

			ByteBuffer buffForReceive = ByteBuffer.allocate(BUFSIZ);

			while (!Thread.currentThread().isInterrupted()) {
				try {

					socketLocale.receive(buffForReceive);
					/* Appel BLOQUANT */

					buffForReceive.flip();
					blockingQueue.put(charset.decode(buffForReceive).toString());
					buffForReceive.clear();

				} catch (IOException e) {
					System.out.println("Dans" + Thread.currentThread().getName() + " il y a une IOException");
					throw new UncheckedIOException(e);
				} catch (InterruptedException e) {
					return;
				}
			}
		});
		listenerThread.setName("listenerThread");

		listenerThread.start();

		while (scanner.hasNext()) {

			String get = null;
			String next = scanner.nextLine();

			do {
				get = null;
				socketLocale.send(charset.encode(next), adresseDestination);
				try {
					get = blockingQueue.poll(1, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					System.out.println("interruption!!!");
					listenerThread.interrupt();
					return;
				}
				if (get == null) {
					System.out.println("Le serveur n'a pas répondu dans la seconde. ON renvoie le meme");
					socketLocale.send(charset.encode(next), adresseDestination);
				} else {
					System.out.println(get);
				}
			} while (get == null);

		}

		listenerThread.interrupt();

		scanner.close();

		socketLocale.close();
	}
}
