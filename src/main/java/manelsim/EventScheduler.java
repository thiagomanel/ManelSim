package manelsim;

import java.util.HashMap;
import java.util.Map;

import manelsim.Time.Unit;

public final class EventScheduler {

	private static Time emulationStart = null;
	private static Time emulationEnd = null;
	private static EventSourceMultiplexer eventSourceMultiplexer = null;
	private static boolean stopOnError = true;
	private static Time now = new Time(0L, Unit.MILLISECONDS);
	private static Map<String, Long> eventsCountByType = new HashMap<String, Long>();

	private EventScheduler() { }

	public static void reset() {
		emulationStart = null;
		emulationEnd = null;
		eventSourceMultiplexer = null;
		eventsCountByType = new HashMap<String, Long>();
		now = new Time(0L, Unit.MILLISECONDS);
	}

	public static void setup(Time emulationStart, Time emulationEnd, EventSourceMultiplexer eventSource) {
		setup(emulationStart, emulationEnd, eventSource, true);
	}

	public static void setup(Time emulationStart, Time emulationEnd, EventSourceMultiplexer eventSource, boolean stopOnError) {
		reset();
		EventScheduler.stopOnError = stopOnError;
		EventScheduler.emulationStart = emulationStart;
		EventScheduler.emulationEnd = emulationEnd;
		EventScheduler.eventSourceMultiplexer = eventSource;
	}

	private static boolean isConfigured() {
		return !(emulationStart == null || emulationEnd == null || eventSourceMultiplexer == null);
	}

	public static void start() {

		if(!isConfigured()) {
			throw new IllegalStateException("EventScheduler is not configured. " +
					"Are you sure you called EventScheduler.setup()?");
		}

		Event nextEvent;

		while ((nextEvent = eventSourceMultiplexer.getNextEvent()) != null && isEarlierThanEmulationEnd(now())) {
			Time eventTime = nextEvent.getScheduledTime();

			if (eventTime.isEarlierThan(now())) {
				String msg = "ERROR: emulation time(" + now()
						+ ") " + "already ahead of event time("
						+ eventTime
						+ "). Event is outdated and will not be processed.";

				if(stopOnError) {
					throw new RuntimeException(msg);
				} else {
					System.err.println(msg);
				}
			} else if (isEarlierThanEmulationEnd(eventTime)) {
				if(isLaterThanEmulationStart(eventTime)) {
					now = eventTime;
					nextEvent.process();
					nextEvent.setProcessed();
					
					String eventType = nextEvent.getClass().getName();
					Long currentCount = eventsCountByType.get(eventType) == null ? 0 : eventsCountByType.get(eventType);
					eventsCountByType.put(eventType, currentCount + 1);
				}
			} else {
				now = emulationEnd;
			}
		}

	}

	private static boolean isLaterThanEmulationStart(Time eventTime) {
		return !eventTime.isEarlierThan(emulationStart);
	}

	private static boolean isEarlierThanEmulationEnd(Time eventTime) {
		return eventTime.isEarlierThan(emulationEnd);
	}

	public static void schedule(Event event) {
		eventSourceMultiplexer.addNewEvent(event);
	}
	
	public static void cancel(Event event) {
		eventSourceMultiplexer.removeEvent(event);
	}
	
	public static Map<String, Long> eventsCountByType() {
		return new HashMap<String, Long>(eventsCountByType);
	}

	public static long eventsCount() {
		long processCount = 0;
		for(Long count : eventsCountByType.values()) {
			processCount += count;
		}
		return processCount;
	}

	public static Time now() {
		return now;
	}

	public static Time getEmulationStart() {
		return emulationStart;
	}

}
