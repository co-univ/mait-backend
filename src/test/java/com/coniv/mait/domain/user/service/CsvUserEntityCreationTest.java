package com.coniv.mait.domain.user.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.auth.repository.UserEntityRepository;
import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.service.TeamService;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.util.TemporaryPasswordGenerator;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CsvUserEntityCreationTest {

	private final String inputFileName = "cotato-users.csv";
	private final String outputFileName = "src/test/resources/cotato-users-password.xlsx"; // .xlsx 확장자로 변경
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	@Autowired
	private UserEntityRepository userEntityRepository;

	@Autowired
	private TeamEntityRepository teamEntityRepository;

	@Autowired
	private TeamService teamService;

	public List<UserEntity> readUsersFromCsv(String csvFileName, String excelOutputPath) throws IOException {
		List<UserEntity> users = new ArrayList<>();
		List<UserData> userData = new ArrayList<>();

		// CSV 파일 읽기
		try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(csvFileName)) {
			if (inputStream == null) {
				throw new IOException("CSV 파일을 찾을 수 없습니다: " + csvFileName);
			}

			String content = new String(inputStream.readAllBytes());
			String[] lines = content.split("\n");

			// 헤더 행을 건너뛰고 데이터 행부터 읽기
			for (int i = 1; i < lines.length; i++) {
				String line = lines[i].trim();
				if (!line.isEmpty()) {
					String[] values = line.split(",");
					if (values.length >= 2) {
						String name = values[0].trim();
						String email = values[1].trim();

						String temporaryPassword = TemporaryPasswordGenerator.generateTemporaryPassword();

						// 비밀번호 암호화
						String encodedPassword = passwordEncoder.encode(temporaryPassword);

						// UserEntity 생성
						UserEntity user = UserEntity.localLoginUser(email, encodedPassword, name, name);
						users.add(user);

						// 엑셀 파일 생성용 데이터 저장
						userData.add(new UserData(name, email, temporaryPassword));
					}
				}
			}
		}

		// 엑셀 파일 생성
		createExcelFile(userData, excelOutputPath);

		return users;
	}

	/**
	 * 사용자 데이터를 엑셀 파일로 생성 (한글 인코딩 문제 해결)
	 *
	 * @param userData 사용자 데이터 리스트
	 * @param filePath 생성할 엑셀 파일 경로
	 * @throws IOException 파일 생성 중 오류 발생 시
	 */
	private void createExcelFile(List<UserData> userData, String filePath) throws IOException {
		try (Workbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("사용자 정보");

			// 헤더 행 생성
			Row headerRow = sheet.createRow(0);
			headerRow.createCell(0).setCellValue("이름");
			headerRow.createCell(1).setCellValue("이메일");
			headerRow.createCell(2).setCellValue("임시 비밀번호");

			// 데이터 행 생성
			for (int i = 0; i < userData.size(); i++) {
				Row row = sheet.createRow(i + 1);
				UserData data = userData.get(i);

				row.createCell(0).setCellValue(data.name);
				row.createCell(1).setCellValue(data.email);
				row.createCell(2).setCellValue(data.temporaryPassword);
			}

			// 열 너비 자동 조정
			for (int i = 0; i < 3; i++) {
				sheet.autoSizeColumn(i);
			}

			// 파일 저장
			try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
				workbook.write(outputStream);
			}
		}
	}

	@Test
	@DisplayName("CSV에서 사용자를 생성하고 팀과 연결")
	void integrationTestWithTeamService() throws IOException {
		List<UserEntity> users = readUsersFromCsv(inputFileName, outputFileName);
		userEntityRepository.saveAll(users);

		List<TeamEntity> teams = teamEntityRepository.findAll();
		TeamEntity team;
		if (teams.isEmpty()) {
			team = TeamEntity.of("테스트");
			teamEntityRepository.save(team);
		} else {
			team = teams.getFirst();
		}

		teamService.createUsersAndLinkTeam(users, team);
	}

	record UserData(String name, String email, String temporaryPassword) {
	}
}
