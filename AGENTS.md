# letsdo-create-io — 작업 안내서

Forge 1.20.1(47.4.0) 서버 전용 애드온. Let's Do 모드의 레시피 처리 블록 엔티티에 Forge `IItemHandler` 능력을 붙여
Create 깔때기·슈트·패키저·호퍼가 아이템을 넣고 뺄 수 있게 한다. 새 블록·아이템·에셋 없음.

## 원칙
- Let's Do 클래스를 컴파일 시점에 직접 참조하지 않는다(설치 안 된 모드가 있어도 로드돼야 함). 블록 엔티티 타입은 레지스트리 ID 문자열로 고르고, 런타임에 `net.minecraft.world.Container`(또는 `WorldlyContainer`) 구현 여부로 감싼다.
- 이미 `ForgeCapabilities.ITEM_HANDLER`를 내는 블록 엔티티는 건드리지 않는다.
- 면별 넣기는 원본 블록의 `WorldlyContainer` 면 규칙을 따르고, 결과 칸은 모든 면에서 뺀다. 필수 이웃 블록이 원본 입력 면을 가리는 경우만 `SlotLayout.sideRoutes`로 옆면 경로를 둔다.
- 슬롯 의미는 블록별 표(코드 안 정적 표 + `config/letsdo_create_io-common.toml`로 끄기 가능)로 정한다: 입력 칸·연료 칸은 넣기만, 결과 칸은 빼기만. 표에 없는 블록은 손대지 않는다.
- 넣기는 해당 블록의 `canPlaceItem`·최대 스택을 지키고, 값을 바꾼 뒤 `setChanged()`와 그 블록이 쓰는 동기화(블록 업데이트 전송)를 부른다.
- 액체는 내부 물 수위가 있는 블록(`SupportedBlocks.WATER`)에만 채우기 전용 Forge 물 탱크로 연결한다(40mB=1칸, NBT 저장·로드 왕복).
- 손으로 젓기·크랭크 같은 조작은 추가하지 않는다.
- Quality Food가 있으면 결과 칸 추출 때 블록의 요리 품질 대기열을 적용한다(찻주전자). 직접 참조는 `QualityFoodBridge`에만 둔다.
- Let's Do 에셋(ARR)은 복사하지 않는다. 코드는 MIT.

## 범위·기록 한도
1. 기존 결함은 이번 목표 산출물·수용 기준을 막을 때만 고친다. 그 밖은 보고서 후보 목록에만 남긴다.
2. 구현은 한 작업자. 독립 검토 에이전트는 최대 1개.
3. 증거는 수용 판정용(빌드·GameTest 결과)만 남긴다.
4. 보고는 띄어쓰기 지킨 평문으로 짧게.

## 명령
- JDK 17: `/mnt/games/sunlit-valley/java17` (또는 시스템 JDK 17)
- 대상 모드 jar 원본: `/mnt/games/sunlit-valley/runtime/mods/letsdo-*.jar`, `architectury-9.2.14-forge.jar`(읽기 전용, 이 폴더 `libs/`에 복사해서 사용, 커밋 금지 → .gitignore)
- 운영 서버(`/mnt/games/sunlit-valley/runtime`)는 절대 수정·재시작하지 않는다.

## 모듈 지도
- `src/main/java/tv/byongshin/letsdocreateio/SupportedBlocks.java`: 조사된 블록 엔티티 ID와 슬롯 표.
- `SlotLayout.java`: 불변 슬롯 역할과 삽입 수량 계산.
- `ContainerItemHandler.java`: Container 어댑터, 시뮬레이션과 스택 제한.
- `LetsDoCreateIO.java`: common 설정, 능력 중복 확인·부착·무효화, 변경 동기화.
- `SiloSlots.java`: 사일로 멀티블록의 공개 getControllerBE·getCapacity만 리플렉션으로 호출해 모든 세그먼트를 컨트롤러로 전달.
- `src/test/`: JUnit 슬롯 로직 검증.
- `src/gametest/`: 실제 모드 블록 배치와 입출력 GameTest. 배포 jar에서 제외.
- `README.md`: 지원 슬롯, 기존 능력 보존 대상, 비 Container·비처리 블록 조사 결과.

## 검증 명령
- 컴파일러가 있는 JDK 17을 `JAVA_HOME`으로 지정한다. 운영 java17 경로는 JRE여서 빌드에 부족하다.
- `./gradlew build`: JUnit과 배포 jar 빌드·재난독화.
- `./gradlew runGameTestServer`: libs의 로컬 모드를 fg.deobf 런타임 의존성으로 로드한 통합 검증.
- 전체 모드 검증에는 기존 지정 jar 외에 WilderNature 의존성인 Curios도 libs로 복사한다.
- 결과: `build/libs/letsdo_create_io-1.3.0.jar`, `build/reports/tests/test/`, `run/logs/latest.log`.
