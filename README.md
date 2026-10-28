# 별잇기 (Byeolitgi)

별의 배치만 보고 별자리를 유추해 직접 선으로 잇는 학습형 별자리 게임입니다.
이름도 연결선도 없는 실제 별 배치를 관찰하고, 별자리를 떠올려 두 별씩 이어 제출하면
연결 순서·방향과 관계없이 기준 도안과 비교해 정답을 판정합니다.

> 소프트웨어시스템설계 개인 프로젝트 (2026 가을) · Java 17 · Swing

## 주요 기능

- **두 가지 별자리 체계**: IAU 88개 별자리, 천상열차분야지도 기반 3원 28수 성관
- **기본 문제 모드**: 쉬움·보통·어려움 난이도로 한 문제씩 연습 (점수 없음)
- **밤하늘 모드**: 위치·날짜·시간을 입력하면 실제 그 시각의 하늘을 계산해 보여 주고,
  지평선 위에 있는 별자리를 찾아 이을 때마다 점수 획득 (쉬움 10 · 보통 20 · 어려움 30점)
- **힌트와 정답 보기**: 막히면 선 하나를 힌트로 받거나(점수 50%) 정답 도안을 공개(0점)
- **자동 저장**: 진행 상황을 `~/.byeolitgi/save.properties` 에 저장하고 다음 실행 때 이어 하기

## 실행

JDK 17 이상이 필요합니다.

```sh
# 배포용 jar 실행
java -jar ssd_ip_cp4_2026f_233575_app.jar

# 소스에서 빌드 후 실행 (Windows)
run.bat

# 판정·저장 로직 점검 (20개 항목)
run.bat check
```

## 조작

| 동작 | 방법 |
| --- | --- |
| 선 잇기 | 별 클릭 → 다른 별 클릭 (이어서 계속 연결) |
| 선 삭제 | 선 위에서 오른쪽 클릭 |
| 선택 해제 | 빈 곳 클릭 또는 `Esc` |
| 실행 취소 | `Ctrl+Z` 또는 [실행 취소] 버튼 |
| 화면 이동 / 확대·축소 | 드래그 / 마우스 휠 |

## 플레이 팁: 별자리 목록을 옆에 두고 푸세요

별잇기는 이름도 선도 없는 별 배치만 보고 풀어야 해서, 별자리를 잘 모르면 **꽤 어렵습니다.**
처음에는 별자리 목록이나 성도를 옆에 띄워 두고 **모양을 대조해 보면서** 푸는 것을 추천합니다.
익숙해지면 목록 없이 도전해 보세요.

- 게임의 도안은 [Stellarium](https://stellarium.org/)의 스카이컬처를 기준으로 합니다.
  IAU 별자리는 *Modern*, 3원 28수는 *Korean* 스카이컬처와 같은 모양이므로
  Stellarium(또는 [Stellarium Web](https://stellarium-web.org/))에서 해당 스카이컬처를 켜 두고 비교하면 가장 정확합니다.
- IAU 88개 별자리 목록: [IAU – The Constellations](https://www.iau.org/public/themes/constellations/),
  [Wikipedia – IAU designated constellations](https://en.wikipedia.org/wiki/IAU_designated_constellations)
- 3원 28수: [위키백과 – 28수](https://ko.wikipedia.org/wiki/28수)
- 밤하늘 모드에서는 별을 하나 선택한 뒤 힌트를 누르면 그 별이 속한 별자리의 선을 알려 줍니다.

## 프로젝트 구조

```
src/byeolitgi/
├── Main.java          실행 진입점
├── ui/                Swing 화면 (MainFrame, StartPanel, SetupPanel, ObservationPanel, GamePanel, StarCanvas …)
├── game/              게임 진행 (GameSession, BasicGame, NightSkyGame, EdgeBoard, AnswerJudge …)
├── model/             엔티티 (Catalog, Constellation, Star, Edge, Observation, 열거형)
├── policy/            규칙 값 (난이도 분류, 점수, 힌트 횟수, 야간·관측 기준)
├── astro/             천문 계산 (항성시, 지평 좌표, 태양 고도, 투영)
├── data/              데이터 파일 읽기 + stars.txt, constellations_iau.txt, constellations_korean.txt
└── save/              진행 상황 저장·불러오기
test/byeolitgi/LogicCheck.java   판정·힌트·저장 로직 점검
doc/                   보고서, UMLet 다이어그램(.uxf), 그림(diagram/, UI/, api/)
```

계층은 `ui → game → policy·astro → model` 방향으로만 의존합니다.
클래스 다이어그램은 `doc/CD-0*.uxf`(UMLet), 그림은 `doc/diagram/` 에 있습니다.

## API 문서 (Javadoc)

```sh
javadoc -encoding UTF-8 -charset UTF-8 -docencoding UTF-8 -package \
        -overview src/overview.html -d dist/api_manual -sourcepath src -subpackages byeolitgi
```

`dist/api_manual/index.html` 을 열면 됩니다.
