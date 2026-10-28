# letsdo-create-io — 작업 안내서

Forge 1.20.1(47.4.0) 서버 전용 애드온. Let's Do 모드의 레시피 처리 블록 엔티티에 Forge `IItemHandler` 능력을 붙여
Create 깔때기·슈트·패키저·호퍼가 아이템을 넣고 뺄 수 있게 한다. 새 블록·아이템·에셋 없음.

## 원칙
- Let's Do 클래스를 컴파일 시점에 직접 참조하지 않는다(설치 안 된 모드가 있어도 로드돼야 함). 블록 엔티티 타입은 레지스트리 ID 문자열로 고르고, 런타임에 `net.minecraft.world.Container`(또는 `WorldlyContainer`) 구현 여부로 감싼다.
- 이미 `ForgeCapabilities.ITEM_HANDLER`를 내는 블록 엔티티는 건드리지 않는다.
- 슬롯 의미는 블록별 표(코드 안 정적 표 + `config/letsdo_create_io-common.toml`로 끄기 가능)로 정한다: 입력 칸·연료 칸은 넣기만, 결과 칸은 빼기만. 표에 없는 블록은 손대지 않는다.
- 넣기는 해당 블록의 `canPlaceItem`·최대 스택을 지키고, 값을 바꾼 뒤 `setChanged()`와 그 블록이 쓰는 동기화(블록 업데이트 전송)를 부른다.
- 손으로 젓기·크랭크 같은 조작은 추가하지 않는다.
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
