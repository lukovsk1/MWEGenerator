package utility;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public final class StatsUtility {

    private static final long MILLISECONDS_IN_HOUR = 60L * 60L * 1000L;
    private static final long MILLISECONDS_IN_MINUTE = 60L * 1000L;
    private static final long MILLISECONDS_IN_SECOND = 1000L;

    private static StatsTracker statTracker;

    private StatsUtility() {
    }

    public static StatsTracker getStatsTracker() {
        return statTracker;
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

    public static StatsTracker initStatsTracker(String formattedDate) {
        StatsUtility.statTracker = new StatsTracker(formattedDate);
        return StatsUtility.statTracker;
    }

    public static void trackInputSize(File sourceFolder) {
        getStatsTracker().writeInputFileSize(FileUtils.sizeOfDirectory(sourceFolder));
        try (Stream<Path> walk = Files.walk(sourceFolder.toPath())) {
            long lineCount = walk
                    .filter(path -> Files.isRegularFile(path) && "java".equals(FilenameUtils.getExtension(path.toString())))
                    .flatMap(p -> {
                        try {
                            return Files.lines(p);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .parallel()
                    .count();
            getStatsTracker().writeInputLOC(lineCount);
        } catch (Exception e) {
            throw new StatsException("Exception occurred while calculation input file size", e);
        }
    }

    public static void trackOutputSize(File outputFolder) {
        getStatsTracker().writeOutputFileSize(FileUtils.sizeOfDirectory(outputFolder));
        try (Stream<Path> walk = Files.walk(outputFolder.toPath())) {
            long lineCount = walk
                    .filter(path -> Files.isRegularFile(path) && "java".equals(FilenameUtils.getExtension(path.toString())))
                    .flatMap(p -> {
                        try {
                            return Files.lines(p);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .parallel()
                    .count();
            getStatsTracker().writeOutputLOC(lineCount);
        } catch (Exception e) {
            throw new StatsException("Exception occurred while calculation input file size", e);
        }
    }

}
