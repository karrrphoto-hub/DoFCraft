# Архивирование и удаление экспериментального Макро

## Сохранённый архив

- Репозиторий: https://github.com/karrrphoto-hub/DOF-Calculator
- Приватность подтверждена владельцем в интерфейсе GitHub.
- Архивная ветка: `archive/macro-prototype`.
- SHA: `e92688b2ee250d12e19e3d48cd461b08a0ecc3ba`.
- После загрузки выполнен отдельный Fetch через авторизованную Android Studio.
  `FETCH_HEAD`, локальная архивная ветка и `origin/archive/macro-prototype`
  указали на один SHA. Содержимое этого коммита проверено до удаления.
- Полная техническая документация находится в `docs/MACRO_RESEARCH.md`
  архивной ветки. Она описывает фактическую реализацию, формулы, ограничения
  точности, исходные файлы, тесты и возможные направления продолжения.

В архиве сохранён весь проект: автоматическая оценка и ручной масштаб, связанная
дистанция, эквивалентное фокусное, эффективная диафрагма, проверки согласованности,
макропоправка дифракции, информационное предупреждение, UI, настройки и тесты.
Модель тонкой линзы ограничена внутренней фокусировкой, положением главных
плоскостей и неизвестным отношением зрачков. Отдельного точного ядра макро-ГРИП нет.

## Основная версия

Основная рабочая ветка — `main`. Удалены макрокнопка, редактор масштаба,
подтверждения, автоматический макропромпт, соответствующие состояния и
обработчики ViewModel, математические модули и ресурсы. Обычная справка о
дифракции вынесена в `DiffractionInfoSheet.kt`.

Дифракция использует установленную диафрагму:
`airyDiameterMm = 0.001342 × aperture`,
`ratio = airyDiameterMm / effectiveCoCmm`.
После последующего обновления предупреждений действуют пороги: NONE < 1.0,
POSSIBLE от 1.0 до 1.5, NOTICEABLE от 1.5.
Поправка на масштаб больше не вычисляется. Ограничение для больших масштабов
описано только в подробной справке на русском и английском.

В DataStore сохранены объявления старых ключей и сами неиспользуемые значения.
Никакой очистки хранилища нет. Старый Manual в обычной ГРИП больше не активируется;
неизвестное сохранённое имя режима Macro читается как ГРИП. Сохранённые ГРИП и
Hyperfocal продолжают восстанавливаться. Положительная фактическая дистанция
сохраняется даже тогда, когда на ней нет результата обычной геометрической модели.

Код ГРИП/гиперфокала, шесть критериев CoC, шкала слайдера, каталоги камер,
пользовательские объективы и их хранилище, applicationId, SDK, манифест и
зависимости не изменены.

## Проверки 2026-11-08

- Gradle Sync: успешно.
- `:app:assembleDebug`, Kotlin-компиляция: успешно.
- `:app:assembleDebugAndroidTest`: успешно.
- `:app:lintDebug`: успешно.
- Все локальные unit-тесты: **185 passed**, 0 failed / skipped.
- Весь набор Android-тестов: **19 passed** на `emulator-5554`.
- Проверены локализации, светлая/тёмная темы, крупный шрифт, обычная дифракция,
  отсутствие макроэлементов, ГРИП/гиперфокал, Infinity, критерии резкости и объективы.
- Тесты проверяют сохранение legacy-ключей, чтение старого режима, отсутствие
  макропоправки, независимость Airy от фокусного/дистанции и сохранение CoC/оптики.
- `git diff --check`: без ошибок.
- Проверка сигнатур секретов и состава публикуемых файлов: находок нет.
  Проверка не является гарантией распознавания произвольных секретов.
- Инструментальные тесты на личном Honor не запускались. Его данные не очищались.
- Авторизация GitHub доступна в IDE; прямой терминальный/API доступ отсутствует.
  Проверка архива выполнена по фактическому Fetch, а Private — по подтверждению
  владельца. Публикация/проверка очищенного `main` отражается в итоговом отчёте.

## Восстановление отдельной рабочей копии

```sh
git fetch origin
git worktree add ../DOF-Calculator-Macro archive/macro-prototype
```

Команда оставляет текущую рабочую копию и основную ветку на месте.

## Файлы

Добавлены (2):

- `app/src/main/java/com/example/dofcalculator/ui/calculator/DiffractionInfoSheet.kt`
- `docs/MACRO_REMOVAL.md`

Изменены (27):

- `AGENTS.md`
- `app/src/androidTest/java/com/example/dofcalculator/DiffractionUiTest.kt`
- `app/src/androidTest/java/com/example/dofcalculator/FocusDistanceDialogsTest.kt`
- `app/src/main/java/com/example/dofcalculator/calculation/DiffractionCalculator.kt`
- `app/src/main/java/com/example/dofcalculator/model/DiffractionResult.kt`
- `app/src/main/java/com/example/dofcalculator/settings/AppSettings.kt`
- `app/src/main/java/com/example/dofcalculator/settings/DataStoreAppSettingsRepository.kt`
- `app/src/main/java/com/example/dofcalculator/settings/DiffractionSettings.kt`
- `app/src/main/java/com/example/dofcalculator/settings/InMemoryAppSettingsRepository.kt`
- `app/src/main/java/com/example/dofcalculator/ui/calculator/AppDialogs.kt`
- `app/src/main/java/com/example/dofcalculator/ui/calculator/CompactCalculatorContent.kt`
- `app/src/main/java/com/example/dofcalculator/ui/calculator/DiffractionWarning.kt`
- `app/src/main/java/com/example/dofcalculator/ui/calculator/DistanceParameterTile.kt`
- `app/src/main/java/com/example/dofcalculator/ui/calculator/DofCalculatorScreen.kt`
- `app/src/main/java/com/example/dofcalculator/ui/calculator/DofCalculatorUiState.kt`
- `app/src/main/java/com/example/dofcalculator/ui/calculator/DofCalculatorViewModel.kt`
- `app/src/main/java/com/example/dofcalculator/ui/calculator/FocusDistanceDialogs.kt`
- `app/src/main/res/values-ru/strings.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/test/java/com/example/dofcalculator/calculation/DiffractionCalculatorTest.kt`
- `app/src/test/java/com/example/dofcalculator/settings/DiffractionPersistenceTest.kt`
- `app/src/test/java/com/example/dofcalculator/settings/VideoSettingsPersistenceTest.kt`
- `app/src/test/java/com/example/dofcalculator/ui/calculator/CameraLensesViewModelTest.kt`
- `app/src/test/java/com/example/dofcalculator/ui/calculator/DiffractionViewModelTest.kt`
- `app/src/test/java/com/example/dofcalculator/ui/calculator/StandardCoCViewModelTest.kt`
- `app/src/test/java/com/example/dofcalculator/ui/calculator/VideoSharpnessViewModelTest.kt`
- `docs/MACRO_RESEARCH.md`

Удалены из main, сохранены в архиве (20):

- `app/src/androidTest/java/com/example/dofcalculator/MacroWarningUiTest.kt`
- `app/src/main/java/com/example/dofcalculator/calculation/MacroWarningEvaluator.kt`
- `app/src/main/java/com/example/dofcalculator/calculation/MagnificationEstimator.kt`
- `app/src/main/java/com/example/dofcalculator/calculation/ManualMagnificationValidator.kt`
- `app/src/main/java/com/example/dofcalculator/calculation/ThinLensEquivalentCalculator.kt`
- `app/src/main/java/com/example/dofcalculator/model/MacroDetectionState.kt`
- `app/src/main/java/com/example/dofcalculator/model/MagnificationMode.kt`
- `app/src/main/java/com/example/dofcalculator/model/ManualMagnificationState.kt`
- `app/src/main/java/com/example/dofcalculator/ui/calculator/MacroFormUiState.kt`
- `app/src/main/java/com/example/dofcalculator/ui/calculator/MacroWarningCard.kt`
- `app/src/main/java/com/example/dofcalculator/ui/calculator/MagnificationFormatter.kt`
- `app/src/main/java/com/example/dofcalculator/ui/calculator/MagnificationSettingsSheet.kt`
- `app/src/test/java/com/example/dofcalculator/calculation/MacroWarningEvaluatorTest.kt`
- `app/src/test/java/com/example/dofcalculator/calculation/MagnificationEstimatorTest.kt`
- `app/src/test/java/com/example/dofcalculator/calculation/ThinLensEquivalentCalculatorTest.kt`
- `app/src/test/java/com/example/dofcalculator/settings/MacroWarningPersistenceTest.kt`
- `app/src/test/java/com/example/dofcalculator/settings/ManualMacroPersistenceTest.kt`
- `app/src/test/java/com/example/dofcalculator/ui/calculator/MacroFocusDistanceTest.kt`
- `app/src/test/java/com/example/dofcalculator/ui/calculator/MacroWarningViewModelTest.kt`
- `app/src/test/java/com/example/dofcalculator/ui/calculator/MagnificationFormatterTest.kt`
