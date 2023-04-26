package utility;

import org.apache.poi.ss.usermodel.CellCopyPolicy;
import org.apache.poi.xssf.usermodel.*;
import testexecutor.TestExecutorOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class StatsTracker {
    private static final String WORKBOOK_PATH = File.separator + "stats" + File.separator + "Statistics.xlsx";

    private final XSSFWorkbook workbook;
    private final XSSFSheet m_sheet;
    private XSSFRow m_activeDDminRow;
    private int m_ddminRowNumber = 31;
    private int m_compilerCalls = 0;
    private int m_failedRuns = 0;

    public StatsTracker(String formattedDate) {
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
            m_sheet = workbook.cloneSheet(sheetIndex, formattedDate);
        } catch (Exception e) {
            throw new StatsException("Error while initializing StatsTracker", e);
        }
    }

    // get cell with real coordinates (starting from 1)
    private XSSFCell getCell(int rowNo, int columnNo) {
        XSSFRow row = m_sheet.getRow(rowNo - 1);
        if (row == null) {
            row = m_sheet.createRow(rowNo - 1);
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

    public void saveStats(long startTime) {
        // write total execution time
        getCell(26, 2).setCellValue(System.currentTimeMillis() - startTime);

        // delete trailing rows
        for (int i = m_ddminRowNumber; i <= m_sheet.getLastRowNum(); i++) {
            m_sheet.removeRow(m_sheet.getRow(i));
        }

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

    public void writeGeneratorName(String simpleName) {
        getCell(1, 2).setCellValue(simpleName);
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


    public void startTrackingDDminExecution(String levelIdentifier, long numberOfFragmentsForDDmin, long totalFragments) {
        // duplicate the last row with its formula
        m_sheet.copyRows(m_ddminRowNumber, m_ddminRowNumber, m_ddminRowNumber + 1, new CellCopyPolicy());

        // start writing values in the originally last row of the document
        m_activeDDminRow = m_sheet.getRow(m_ddminRowNumber);
        getCell(m_activeDDminRow, 1).setCellValue(levelIdentifier);
        getCell(m_activeDDminRow, 2).setCellValue(numberOfFragmentsForDDmin);
        getCell(m_activeDDminRow, 5).setCellValue(totalFragments);
    }

    public void trackDDminExecutionEnd(long startTime, long minConfigSize, long totalFragmentsLeft) {
        getCell(23, 2).setCellValue(totalFragmentsLeft);

        if (m_activeDDminRow == null) {
            return;
        }
        getCell(m_activeDDminRow, 3).setCellValue(minConfigSize);
        getCell(m_activeDDminRow, 6).setCellValue(totalFragmentsLeft);
        getCell(m_activeDDminRow, 10).setCellValue(System.currentTimeMillis() - startTime);
        m_ddminRowNumber++;
    }

    public void trackDDMinCompilerStats(int compilerCallsTotal, int failedRunsTotal) {
        // write global stats
        getCell(27, 2).setCellValue(compilerCallsTotal);
        getCell(28, 2).setCellValue(failedRunsTotal);

        // write deltas to run row
        if (m_activeDDminRow == null) {
            return;
        }

        getCell(m_activeDDminRow, 8).setCellValue(compilerCallsTotal - m_compilerCalls);
        getCell(m_activeDDminRow, 9).setCellValue(failedRunsTotal - m_failedRuns);
        m_compilerCalls = compilerCallsTotal;
        m_failedRuns = failedRunsTotal;
    }
}
