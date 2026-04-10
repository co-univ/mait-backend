import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

const idsFile = __ENV.IDS_FILE || './question-set-ids.sample.json';
const targetGroup = __ENV.TARGET_GROUP || 'liveAfter';
const payload = JSON.parse(open(idsFile));
const questionSetIds = payload[targetGroup] || [];

if (questionSetIds.length === 0) {
	throw new Error(`No question set ids found for TARGET_GROUP=${targetGroup} in ${idsFile}`);
}

export const deleteLatency = new Trend('question_set_delete_latency', true);

export const options = {
	scenarios: {
		delete_question_sets: {
			executor: 'shared-iterations',
			vus: Number(__ENV.VUS || 1),
			iterations: Number(__ENV.ITERATIONS || questionSetIds.length),
			maxDuration: __ENV.MAX_DURATION || '10m',
		},
	},
	thresholds: {
		http_req_failed: ['rate<0.01'],
		http_req_duration: ['p(95)<5000'],
		question_set_delete_latency: ['p(95)<5000'],
	},
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const token = __ENV.ACCESS_TOKEN;

if (!token) {
	throw new Error('ACCESS_TOKEN env is required');
}

export default function () {
	const questionSetId = questionSetIds[__ITER % questionSetIds.length];
	const response = http.del(`${baseUrl}/api/v1/question-sets/${questionSetId}`, null, {
		headers: {
			Authorization: `Bearer ${token}`,
		},
		tags: {
			target_group: targetGroup,
			question_set_id: String(questionSetId),
		},
	});

	deleteLatency.add(response.timings.duration, {
		target_group: targetGroup,
		question_set_id: String(questionSetId),
	});

	check(response, {
		'delete status is 200': (res) => res.status === 200,
	});

	sleep(Number(__ENV.SLEEP_SECONDS || 0));
}
