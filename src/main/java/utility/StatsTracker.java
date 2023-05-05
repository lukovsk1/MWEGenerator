package utility;

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.CellCopyPolicy;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.*;
import org.openxmlformats.schemas.drawingml.x2006.chart.*;
import testexecutor.TestExecutorOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Objects;

public class StatsTracker {

	private static final String STATS_FOLDER = File.separator + "stats" + File.separator;
	private static final String FALLBACK_WORKBOOK_PATH = STATS_FOLDER + "Statistics.xlsx";
	private static final String SHEET_NAME_OVERVIEW = "Overview";
	private static final String SHEET_NAME_TEMPLATE = "Template";
	private static final String MWE_GENERATOR_SUFFIX = "MWEGenerator";
	private final String m_formattedDate;
	private final long m_trackerStartTime;

	private File m_workbookFile;
	private XSSFWorkbook m_workbook;
	private XSSFSheet m_sheet;
	private XSSFRow m_activeDDminRow;
	private int m_ddminRowNumber = 31;
	private int m_compilerCalls = 0;
	private int m_failedRuns = 0;
	private String m_testCase;
	private String m_algorithmName;


	public StatsTracker(String formattedDate) {
		m_formattedDate = formattedDate;
		m_trackerStartTime = System.currentTimeMillis();
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

		updateFormulas();
		updateCharts();

		try {
			FileOutputStream fos = new FileOutputStream(m_workbookFile);
			m_workbook.write(fos);
			m_workbook.close();
		} catch (Exception e) {
			throw new StatsException("Unable to write stats to excel file " + m_workbookFile, e);
		}
	}

	private void updateFormulas() {
		XSSFCell runNumberCell = getCell(20, 4);
		runNumberCell.setCellFormula("COUNTA(A32:A" + m_ddminRowNumber + ")");
		XSSFCell averageRunSize = getCell(25, 4);
		averageRunSize.setCellFormula("AVERAGE(B32:B" + m_ddminRowNumber + ")");

		// Evaluate all formulas in the workbook
		XSSFFormulaEvaluator formulaEvaluator = m_workbook.getCreationHelper().createFormulaEvaluator();
		formulaEvaluator.evaluateAll();
	}

	private void updateCharts() {
		// Update fragment number chart on active sheet
		XSSFChart chart = getChart(m_sheet, "# of fragments after # of compiler calls");
		CTChart ctChart = chart.getCTChart();
		CTPlotArea ctPlotArea = ctChart.getPlotArea();
		CTScatterChart scatterChart = ctPlotArea.getScatterChartList().get(0);

		// extract series area references
		CTScatterSer ser = scatterChart.getSerArray(0);
		CTNumRef xNumRef = ser.getXVal().getNumRef();
		CTNumRef yNumRef = ser.getYVal().getNumRef();
		AreaReference xAreaReference = new AreaReference(xNumRef.getF(), SpreadsheetVersion.EXCEL2007);
		AreaReference yAreaReference = new AreaReference(yNumRef.getF(), SpreadsheetVersion.EXCEL2007);

		// override reference of last cell
		CellReference xFirstCell = xAreaReference.getFirstCell();
		CellReference yFirstCell = yAreaReference.getFirstCell();

		xAreaReference = new AreaReference(
				xFirstCell,
				new CellReference(xFirstCell.getSheetName(), m_ddminRowNumber, xFirstCell.getCol(), true, true),
				SpreadsheetVersion.EXCEL2007);
		yAreaReference = new AreaReference(
				yFirstCell,
				new CellReference(yFirstCell.getSheetName(), m_ddminRowNumber, yFirstCell.getCol(), true, true),
				SpreadsheetVersion.EXCEL2007);

		xNumRef.setF(xAreaReference.formatAsString());
		yNumRef.setF(yAreaReference.formatAsString());

        /* TODO: Update fragment number chart on overview sheet
        XSSFSheet overviewSheet = m_workbook.getSheet(SHEET_NAME_OVERVIEW);
        XSSFChart overviewChart = getChart(overviewSheet, m_testCase); // get chart by test case name
        if (overviewChart == null) {
            System.out.println("Unable to find overview chart for test case " + m_testCase);
            return;
        }

        XDDFCategoryAxis xAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis yAxis = chart.createValueAxis(AxisPosition.LEFT);
        XDDFChartData data = overviewChart.createData(ChartTypes.SCATTER, xAxis, yAxis);
        XDDFChartData.Series series = data.addSeries(
                XDDFDataSourcesFactory.fromNumericCellRange(m_sheet, new CellRangeAddress(xFirstCell.getRow(), m_ddminRowNumber, xFirstCell.getCol(), xFirstCell.getCol())),
                XDDFDataSourcesFactory.fromNumericCellRange(m_sheet, new CellRangeAddress(yFirstCell.getRow(), m_ddminRowNumber, yFirstCell.getCol(), yFirstCell.getCol())));
        series.setTitle("test", null);
         */
	}

	private XSSFChart getChart(XSSFSheet sheet, String chartTitle) {
		if (chartTitle == null) {
			return null;
		}
		XSSFDrawing drawing = sheet.createDrawingPatriarch();
		return drawing.getCharts().stream()
				.filter(Objects::nonNull)
				.filter(c -> c.getTitleText() != null)
				.filter(c -> chartTitle.equals(c.getTitleText().toString()))
				.findFirst()
				.orElse(null);
	}

	public void writeRunConfiguration(String generatorName, TestExecutorOptions options) {
		// get test case name
		String modulePath = options.getModulePath();
		if (modulePath.endsWith(File.separator)) {
			modulePath = modulePath.substring(0, modulePath.length() - 1);
		}
		m_testCase = modulePath.substring(modulePath.lastIndexOf(File.separator) + 1);

		String dir = System.getProperty("user.dir");
		m_workbookFile = new File(dir + STATS_FOLDER + m_testCase + ".xlsx");
		if (!m_workbookFile.exists()) {
			m_workbookFile = new File(dir + FALLBACK_WORKBOOK_PATH);
			if (!m_workbookFile.exists()) {
				throw new StatsException("Unable to find stats file " + m_workbookFile);
			}
		}
		try {
			FileInputStream fis = new FileInputStream(m_workbookFile);
			m_workbook = new XSSFWorkbook(fis);
			int sheetIndex = m_workbook.getSheetIndex(SHEET_NAME_TEMPLATE);
			if (sheetIndex < 0) {
				throw new StatsException("Unable to find template sheet");
			}
			m_sheet = m_workbook.cloneSheet(sheetIndex, m_formattedDate);
		} catch (Exception e) {
			throw new StatsException("Error while initializing StatsTracker", e);
		}
		getCell(1, 4).setCellValue(m_testCase);

		// write executor options
		getCell(4, 2).setCellValue(modulePath);
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

		m_algorithmName = generatorName.substring(0, generatorName.length() - MWE_GENERATOR_SUFFIX.length());
		getCell(1, 2).setCellValue(m_algorithmName);
		// max sheet name length is 32 chars
		String shortTestCaseName = shortenString(m_testCase, 30 - m_algorithmName.length() - m_formattedDate.length());
		m_workbook.setSheetName(m_workbook.getSheetIndex(m_sheet), m_algorithmName + "_" + shortTestCaseName + "_" + m_formattedDate);
	}

	private String shortenString(String str, int length) {
		if (str == null) {
			return "";
		}
		if (str.length() <= length) {
			return str;
		}

		String withoutLowercase = str.replaceAll("[a-z]", "");
		if (0 < withoutLowercase.length() && withoutLowercase.length() <= length) {
			return withoutLowercase;
		}

		return str.substring(0, length);
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
		getCell(m_activeDDminRow, 12).setCellValue(System.currentTimeMillis() - startTime);
		getCell(m_activeDDminRow, 13).setCellValue(System.currentTimeMillis() - m_trackerStartTime);
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
