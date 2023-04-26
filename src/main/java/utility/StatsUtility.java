package utility;

public final class StatsUtility {

    private static final long MILLISECONDS_IN_HOUR = 60L * 60L * 1000L;
    private static final long MILLISECONDS_IN_MINUTE = 60L * 1000L;
    private static final long MILLISECONDS_IN_SECOND = 1000L;

    private StatsUtility() {
    }

    public static String formatDuration(long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        StringBuilder sb = new StringBuilder();

        if (duration >= MILLISECONDS_IN_HOUR) {
            long hours = duration / MILLISECONDS_IN_HOUR;
            sb.append(hours).append("h ");
            duration -= (hours * MILLISECONDS_IN_HOUR);
        }
        if (duration >= MILLISECONDS_IN_MINUTE) {
            long minutes = duration / MILLISECONDS_IN_MINUTE;
            sb.append(minutes).append("m ");
            duration -= (minutes * MILLISECONDS_IN_MINUTE);
        }
        if (duration >= MILLISECONDS_IN_SECOND) {
            long seconds = duration / MILLISECONDS_IN_SECOND;
            sb.append(seconds).append("s ");
            duration -= (seconds * MILLISECONDS_IN_SECOND);
        }
        if (duration >= 0) {
            sb.append(duration).append("ms");
        }

        return sb.toString().trim();
    }
}
