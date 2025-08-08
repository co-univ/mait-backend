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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.service.TeamService;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.util.TemporaryPasswordGenerator;

@SpringBootTest
//@ActiveProfiles("production") //TODO: 프로덕션 yml파일 만들어야함 + 활성화
@ActiveProfiles("test")
class CsvUserEntityCreationTest {

	private final String inputFileName = "cotato-users.csv";
	private final String outputFileName = "src/test/resources/cotato-users-password.xlsx";
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	@Autowired
	private UserEntityRepository userEntityRepository;

	@Autowired
	private TeamEntityRepository teamEntityRepository;

	@Autowired
	private TeamService teamService;

	@Transactional
	public List<UserEntity> readUsersFromCsv(String csvFileName, String excelOutputPath) throws IOException {
		List<UserEntity> users = new ArrayList<>();
		List<UserData> userData = new ArrayList<>();

		try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(csvFileName)) {
			if (inputStream == null) {
				throw new IOException("CSV 파일을 찾을 수 없습니다: " + csvFileName);
			}

			String content = new String(inputStream.readAllBytes());
			String[] lines = content.split("\n");

			for (int i = 1; i < lines.length; i++) {
				String line = lines[i].trim();
				if (!line.isEmpty()) {
					String[] values = line.split(",");
					if (values.length >= 2) {
						String name = values[0].trim();
						String email = values[1].trim();

						String temporaryPassword = TemporaryPasswordGenerator.generateTemporaryPassword();

						String encodedPassword = passwordEncoder.encode(temporaryPassword);

						UserEntity user = UserEntity.localLoginUser(email, encodedPassword, name, name);
						users.add(user);

						userData.add(new UserData(name, email, temporaryPassword));
					}
				}
			}
		}

		// 엑셀 파일 생성
		createExcelFile(userData, excelOutputPath);

		return users;
	}

	private void createExcelFile(List<UserData> userData, String filePath) throws IOException {
		try (Workbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("사용자 정보");

			Row headerRow = sheet.createRow(0);
			headerRow.createCell(0).setCellValue("이름");
			headerRow.createCell(1).setCellValue("이메일");
			headerRow.createCell(2).setCellValue("임시 비밀번호");

			for (int i = 0; i < userData.size(); i++) {
				Row row = sheet.createRow(i + 1);
				UserData data = userData.get(i);

				row.createCell(0).setCellValue(data.name);
				row.createCell(1).setCellValue(data.email);
				row.createCell(2).setCellValue(data.temporaryPassword);
			}

			for (int i = 0; i < 3; i++) {
				sheet.autoSizeColumn(i);
			}

			try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
				workbook.write(outputStream);
			}
		}
	}

	@Disabled("실제 CSV 파일을 읽고 엑셀 파일을 생성하는 테스트는 수동으로 실행") //TODO 실제 테스트 시 삭제해야함
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
