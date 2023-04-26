package utility;

import org.apache.poi.ss.usermodel.CellCopyPolicy;
import org.apache.poi.xssf.usermodel.*;
import testexecutor.TestExecutorOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class StatsTracker {
    private static final String WORKBOOK_PATH = File.separator + "stats" + File.separator + "Statistics.xlsx";

    private final String sheetName;
    private final XSSFWorkbook workbook;
    private final XSSFSheet sheet;
    private XSSFRow activeDDminRow;
    private long outputFragments = Long.MAX_VALUE;
    private int ddminRowNumber = 31;

    public StatsTracker(String formattedDate) {
        sheetName = formattedDate;
        String dir = System.getProperty("user.dir");
        File workbookFile = new File(dir + WORKBOOK_PATH);
        if (!workbookFile.exists()) {
            throw new StatsException("Unable to find stats file " + workbookFile);
        }
        try {
            FileInputStream fis = new FileInputStream(workbookFile);
            workbook = new XSSFWorkbook(fis);
            int sheetIndex = workbook.getSheetIndex("Template");
            if (sheetIndex < 0) {
                throw new StatsException("Unable to find template sheet");
            }
            sheet = workbook.cloneSheet(sheetIndex, sheetName);
        } catch (Exception e) {
            throw new StatsException("Error while initializing StatsTracker", e);
        }
    }

    // get cell with real coordinates (starting from 1)
    private XSSFCell getCell(int rowNo, int columnNo) {
        XSSFRow row = sheet.getRow(rowNo - 1);
        if (row == null) {
            row = sheet.createRow(rowNo - 1);
        }

        return getCell(row, columnNo);
    }

    // get cell with real coordinates (starting from 1)
    private XSSFCell getCell(XSSFRow row, int columnNo) {
        XSSFCell cell = row.getCell(columnNo - 1);
        if (cell == null) {
            cell = row.createCell(columnNo - 1);
        }
        return cell;
    }

    public void saveStats() {
        // Evaluate all formulas in the workbook
        XSSFFormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
        formulaEvaluator.evaluateAll();

        String dir = System.getProperty("user.dir");
        File workbookFile = new File(dir + WORKBOOK_PATH);
        try {
            FileOutputStream fos = new FileOutputStream(workbookFile);
            workbook.write(fos);
            workbook.close();
        } catch (Exception e) {
            throw new StatsException("Unable to write stats to excel file " + workbookFile, e);
        }
    }

    public void writeExecutorOptions(TestExecutorOptions options) {
        // write options from row 4-17
        getCell(4, 2).setCellValue(options.getModulePath());
        getCell(5, 2).setCellValue(options.getSourceFolderPath());
        getCell(6, 2).setCellValue(options.getUnitTestFolderPath());
        getCell(7, 2).setCellValue(options.getUnitTestMethod());
        getCell(8, 2).setCellValue(options.getExpectedResult());
        getCell(9, 2).setCellValue(options.getCompilationType().toString());
        getCell(10, 2).setCellValue(options.getLogLevel().toString());
        getCell(11, 2).setCellValue(options.isLogCompilationErrors());
        getCell(12, 2).setCellValue(options.isLogRuntimeErrors());
        getCell(13, 2).setCellValue(options.isMultipleRuns());
        getCell(14, 2).setCellValue(options.getNumberOfThreads());
        getCell(15, 2).setCellValue(options.isPreSliceCode());
        getCell(16, 2).setCellValue(options.getGraphAlgorithmFragmentLimit());
        getCell(17, 2).setCellValue(options.isEscalatingFragmentLimit());
    }

    public void writeInputFileSize(long byteSize) {
        getCell(21, 2).setCellValue(byteSize);
    }

    public void writeInputLOC(long loc) {
        getCell(22, 2).setCellValue(loc);
    }

    public void writeInputFragments(long size) {
        XSSFCell cell = getCell(20, 2);
        // only write if cell is empty
        if (cell.getRawValue() == null || cell.getRawValue().trim().length() == 0) {
            cell.setCellValue(size);
        }
    }

    public void writeOutputFileSize(long byteSize) {
        getCell(24, 2).setCellValue(byteSize);
    }

    public void writeOutputLOC(long loc) {
        getCell(25, 2).setCellValue(loc);
    }

    public void writeOutputFragments(long size) {
        if (size < outputFragments) {
            outputFragments = size;
            getCell(23, 2).setCellValue(size);
        }
    }


    public void startTrackingDDminExecution(String levelIdentifier, long numberOfFragmentsForDDmin, long totalFragments) {
        // duplicate the last row with its formula
        sheet.copyRows(ddminRowNumber, ddminRowNumber, ddminRowNumber + 1, new CellCopyPolicy());

        // start writing values in the originally last row of the document
        activeDDminRow = sheet.getRow(ddminRowNumber);
        getCell(activeDDminRow, 1).setCellValue(levelIdentifier);
        getCell(activeDDminRow, 2).setCellValue(numberOfFragmentsForDDmin);
        getCell(activeDDminRow, 5).setCellValue(totalFragments);
    }

    public void trackDDminExecutionEnd(long startTime, long minConfigSize, long totalFragmentsLeft) {
        if (activeDDminRow == null) {
            return;
        }
        getCell(activeDDminRow, 3).setCellValue(minConfigSize);
        getCell(activeDDminRow, 6).setCellValue(totalFragmentsLeft);
        getCell(activeDDminRow, 10).setCellValue(System.currentTimeMillis() - startTime);
        ddminRowNumber++;
    }
}
