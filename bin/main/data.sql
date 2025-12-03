-- 필수데이터 : financial_product, term, keyword
-- financial_product (기존 데이터 유지)
INSERT INTO financial_product (product_name, product_type, bank_name, min_amount, max_amount, min_period_months, max_period_months, base_interest_rate, interest_rate_details, bonus_rate_details, compounding_strategy, applicable_tendency)
SELECT
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
    '원금보장형'
WHERE NOT EXISTS (SELECT 1 FROM financial_product WHERE product_name = 'WON플러스 예금');

INSERT INTO financial_product (product_name, product_type, bank_name, min_amount, max_amount, min_period_months, max_period_months, base_interest_rate, interest_rate_details, bonus_rate_details, compounding_strategy, applicable_tendency)
SELECT
           '우리 SUPER주거래 적금',
           'SAVINGS',
           '우리은행',
           NULL, -- 최소 가입금액 없음
           500000, -- 월 최대 50만원
           12, -- 1년
           36, -- 3년
           2.15, -- 기본금리 (1년~3년 모두 2.15%)
           NULL, -- 기간별 기본금리가 동일하므로 details 생략
           '{
               "conditions": [
                   {"name": "급여이체 또는 연금이체 실적 (계약기간 1/2 이상)", "rate": 0.7},
                   {"name": "공과금 자동이체 실적 (계약기간 1/2 이상)", "rate": 0.3},
                   {"name": "우리카드(신용/체크) 결제계좌 지정 및 사용실적 (월 10만원 이상)", "rate": 0.3},
                   {"name": "마케팅 동의(전화 및 SMS) 유지", "rate": 0.1}
               ]
           }',
           'SIMPLE',
           '위험중립형'
WHERE NOT EXISTS (SELECT 1 FROM financial_product WHERE product_name = '우리 SUPER주거래 적금');

-- Term (기존 데이터 유지)
INSERT INTO term (is_required, term_name) SELECT true, '서비스 이용약관' WHERE NOT EXISTS (SELECT 1 FROM term WHERE term_name = '서비스 이용약관');
INSERT INTO term (is_required, term_name) SELECT true, '개인정보 수집 및 이용 동의' WHERE NOT EXISTS (SELECT 1 FROM term WHERE term_name = '개인정보 수집 및 이용 동의');
INSERT INTO term (is_required, term_name) SELECT false, '마케팅 정보 수신 동의' WHERE NOT EXISTS (SELECT 1 FROM term WHERE term_name = '마케팅 정보 수신 동의');

-- Keyword (기존 데이터 유지)
INSERT INTO keyword (name) SELECT '안정적 생활비' WHERE NOT EXISTS (SELECT 1 FROM keyword WHERE name = '안정적 생활비');
INSERT INTO keyword (name) SELECT '목돈 마련' WHERE NOT EXISTS (SELECT 1 FROM keyword WHERE name = '목돈 마련');
INSERT INTO keyword (name) SELECT '비상금 확보' WHERE NOT EXISTS (SELECT 1 FROM keyword WHERE name = '비상금 확보');
INSERT INTO keyword (name) SELECT '증여/상속' WHERE NOT EXISTS (SELECT 1 FROM keyword WHERE name = '증여/상속');
INSERT INTO keyword (name) SELECT '대출 상환' WHERE NOT EXISTS (SELECT 1 FROM keyword WHERE name = '대출 상환');
INSERT INTO keyword (name) SELECT '여행' WHERE NOT EXISTS (SELECT 1 FROM keyword WHERE name = '여행');
INSERT INTO keyword (name) SELECT '가족/교류' WHERE NOT EXISTS (SELECT 1 FROM keyword WHERE name = '가족/교류');
INSERT INTO keyword (name) SELECT '고급 취미' WHERE NOT EXISTS (SELECT 1 FROM keyword WHERE name = '고급 취미');
INSERT INTO keyword (name) SELECT '반려동물' WHERE NOT EXISTS (SELECT 1 FROM keyword WHERE name = '반려동물');
INSERT INTO keyword (name) SELECT '귀농/귀촌' WHERE NOT EXISTS (SELECT 1 FROM keyword WHERE name = '귀농/귀촌');
INSERT INTO keyword (name) SELECT '창업/사업' WHERE NOT EXISTS (SELECT 1 FROM keyword WHERE name = '창업/사업');
INSERT INTO keyword (name) SELECT '재취업/소일거리' WHERE NOT EXISTS (SELECT 1 FROM keyword WHERE name = '재취업/소일거리');
INSERT INTO keyword (name) SELECT '자기계발' WHERE NOT EXISTS (SELECT 1 FROM keyword WHERE name = '자기계발');
INSERT INTO keyword (name) SELECT '봉사/사회공헌' WHERE NOT EXISTS (SELECT 1 FROM keyword WHERE name = '봉사/사회공헌');
INSERT INTO keyword (name) SELECT '건강/의료비' WHERE NOT EXISTS (SELECT 1 FROM keyword WHERE name = '건강/의료비');
INSERT INTO keyword (name) SELECT '편안한 휴식' WHERE NOT EXISTS (SELECT 1 FROM keyword WHERE name = '편안한 휴식');

-- 아래부터 유저 데이터. 필수데이터 아님!!

-- Dummy Asset Data for user_id = 1 (testuser1)
-- user_id 1은 AuthService.initTestUser()에서 생성됩니다.
-- INSERT INTO assets (user_id, type, balance, bank_code) VALUES
-- (1, 'CURRENT', 5000000, '002'),
-- (1, 'SAVING', 1200000, '002'),
-- (1, 'INVEST', 25000000, '240'),
-- (1, 'PENSION', 150000000, '001'),
-- (1, 'AUTOMOBILE', 30000000, NULL),
-- (1, 'REAL_ESTATE', 800000000, NULL),
-- (1, 'LOAN', 300000000, '011');

-- -- Dummy Pension Data for user_id = 1 (linked to the PENSION asset above)
-- INSERT INTO pension (asset_id, updated_at, pension_type, account_name, principal, personal_contrib, contrib_year, total_personal_contrib) VALUES
-- ((SELECT asset_id FROM assets WHERE user_id = 1 AND type = 'PENSION' LIMIT 1), NOW(), 'DC', '우리은행 개인형IRP', 150000000.00, 10000000.00, 2023, 50000000.00);
