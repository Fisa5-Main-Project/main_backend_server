-- H2 데이터베이스는 기본적으로 JSON 타입을 지원하지 않으므로, 관련 정보는 TEXT 타입 컬럼에 JSON 형식의 문자열로 저장합니다.

-- WON플러스 예금 (DEPOSIT)
INSERT INTO financial_product (product_name, product_type, bank_name, min_amount, max_amount, min_period_months, max_period_months, base_interest_rate, interest_rate_details, bonus_rate_details, compounding_strategy, applicable_tendency)
VALUES (
    'WON플러스 예금',
    'DEPOSIT',
    '우리은행',
    10000,
    NULL, -- No max amount
    1,
    36,
    NULL, -- Tiered rates are used instead of a single base rate
    '{
        "tiers": [
            {"months_gte": 1, "months_lt": 3, "rate": 2.55},
            {"months_gte": 3, "months_lt": 6, "rate": 2.60},
            {"months_gte": 6, "months_lt": 12, "rate": 2.75},
            {"months_gte": 12, "months_lt": 13, "rate": 2.80},
            {"months_gte": 13, "months_lt": 24, "rate": 2.70},
            {"months_gte": 24, "months_lt": 37, "rate": 2.40}
        ]
    }',
    NULL, -- No bonus rates
    'SIMPLE',
    '원금보존형'
);

-- WON 적금 (SAVINGS)
INSERT INTO financial_product (product_name, product_type, bank_name, min_amount, max_amount, min_period_months, max_period_months, base_interest_rate, interest_rate_details, bonus_rate_details, compounding_strategy, applicable_tendency)
VALUES (
    'WON 적금',
    'SAVINGS',
    '우리은행',
    NULL, -- No min amount
    500000, -- Max monthly deposit
    12,
    12,
    2.95,
    NULL, -- No tiered rates
    '{
        "conditions": [
            {"name": "WON통장/우리꿈통장에서 출금하여 가입하는 경우", "rate": 0.1},
            {"name": "만기해지 시 우리 오픈뱅킹 서비스에 타행계좌가 등록되어 있는 경우", "rate": 0.1}
        ]
    }',
    'SIMPLE',
    '위험중립형'
);

-- Term (약관)
INSERT INTO term (is_required, term_name) VALUES (true, '서비스 이용약관');
INSERT INTO term (is_required, term_name) VALUES (true, '개인정보 수집 및 이용 동의');
INSERT INTO term (is_required, term_name) VALUES (false, '마케팅 정보 수신 동의');

-- Keyword (키워드)

INSERT INTO keyword (name) VALUES ('안정적 생활비');
INSERT INTO keyword (name) VALUES ('목돈 마련');
INSERT INTO keyword (name) VALUES ('비상금 확보');
INSERT INTO keyword (name) VALUES ('증여/상속');
INSERT INTO keyword (name) VALUES ('대출 상환');
INSERT INTO keyword (name) VALUES ('여행');
INSERT INTO keyword (name) VALUES ('가족/교류');
INSERT INTO keyword (name) VALUES ('고급 취미');
INSERT INTO keyword (name) VALUES ('반려동물');
INSERT INTO keyword (name) VALUES ('귀농/귀촌');
INSERT INTO keyword (name) VALUES ('창업/사업');
INSERT INTO keyword (name) VALUES ('재취업/소일거리');
INSERT INTO keyword (name) VALUES ('자기계발');
INSERT INTO keyword (name) VALUES ('봉사/사회공헌');
INSERT INTO keyword (name) VALUES ('건강/의료비');
INSERT INTO keyword (name) VALUES ('편안한 휴식');