package telran.monitoring.pulse;

import java.net.*;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

import telran.monitoring.pulse.dto.SensorData;

public class PulseSenderAppl {
	private static final int N_PACKETS = 100;
	private static final long TIMEOUT = 500;
	private static final int N_PATIENTS = 5;
	private static final int MIN_PULSE_VALUE = 50;
	private static final int MAX_PULSE_VALUE = 200;
	private static final String HOST = "localhost";
	private static final int PORT = 5000;
	private static Random random = new Random();
	static DatagramSocket socket;

	private static Map<Long, SensorData> map;
	private static final int JUMP_PROBABILITY = 40;
	private static final int JUMP_POSITIVE_PROBABILITY = 50;
	private static final int MIN_JUMP_PERCENT = 10;
	private static final int MAX_JUMP_PERCENT = 20;
	private static final int PATIENT_ID_PRINTED_VALUES = 3;

	public static void main(String[] args) throws Exception {
		map = new HashMap<>();
		socket = new DatagramSocket();
		IntStream.rangeClosed(1, N_PACKETS).forEach(PulseSenderAppl::sendPulse);

	}

	static void sendPulse(int seqNumber) {
		SensorData data = getRandomSensorData(seqNumber);
		String jsonData = data.toString();
		sendDatagramPacket(jsonData);
		try {
			Thread.sleep(TIMEOUT);
		} catch (InterruptedException e) {

		}
	}

	private static void sendDatagramPacket(String jsonData) {
		byte[] buffer = jsonData.getBytes();
		try {
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(HOST), PORT);
			socket.send(packet);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	private static SensorData getRandomSensorData(int seqNumber) {
		long patientId = random.nextInt(1, N_PATIENTS + 1);
		int value = getRandomPulseValue(patientId, seqNumber);
		SensorData res = new SensorData(seqNumber, patientId, value, System.currentTimeMillis());
		if (patientId == PATIENT_ID_PRINTED_VALUES) {
			System.out.println(new String(res.toString()));
		}
		return res;
	}

	private static int getRandomPulseValue(long patientId, int seqNumber) {
		int pulseValue;
		int prevPulseValue = map.containsKey(patientId) ? map.get(patientId).value() : -1;
		if (prevPulseValue == -1) {
			pulseValue = random.nextInt(MIN_PULSE_VALUE, MAX_PULSE_VALUE + 1);
			updateMap(patientId, seqNumber, pulseValue);
		} else {
			pulseValue = random.nextInt(0, 100) < JUMP_PROBABILITY
					? prevPulseValue + getJumpSign()
							* (prevPulseValue * random.nextInt(MIN_JUMP_PERCENT, MAX_JUMP_PERCENT + 1)) / 100
					: prevPulseValue;
			if (pulseValue != prevPulseValue) {
				pulseValue = checkRange(pulseValue);
				updateMap(patientId, seqNumber, pulseValue);
			}
		}
		return pulseValue;
	}

	private static void updateMap(long patientId, int seqNumber, int pulseValue) {
		map.put(patientId, new SensorData(seqNumber, patientId, pulseValue, System.currentTimeMillis()));
	}

	private static int checkRange(int value) {
		if (value < MIN_PULSE_VALUE) {
			value = MIN_PULSE_VALUE;
		}
		if (value > MAX_PULSE_VALUE) {
			value = MAX_PULSE_VALUE;
		}
		return value;
	}

	private static int getJumpSign() {
		return random.nextInt(0, 99) < JUMP_POSITIVE_PROBABILITY ? 1 : -1;
	}

}
