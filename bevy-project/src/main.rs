use bevy::prelude::*;
use rusqlite::{Connection, Result as SQLiteError};
use std::{thread, time::Duration};
use std::sync::{Arc, Mutex, RwLock};
use bevy::utils::hashbrown::HashMap;

const query: &str = r#"
SELECT lt.language_name,
       a.asset_name,
       language_translation.translation
FROM language_translation
JOIN language_lookup lt ON lt.language_id = language_translation.language_id
JOIN asset a ON a.asset_id = language_translation.asset_id;
"#;

fn main() -> SQLiteError<()> {
    let path = "../sqlite/amorphous";
    let map: HashMap<String, LanguageTranslation> = HashMap::new();
    let map_arc = Arc::new(RwLock::new(map));
    let resource_map_clone = Arc::clone(&map_arc);
    let thread_map_clone = Arc::clone(&map_arc);

    // start polling
    thread::spawn(move || {
        let mut connection = Connection::open(path).unwrap();
        loop {
            let language_translations = poll(&mut connection);

            match thread_map_clone.try_write() {
                Ok(mut map) => {
                    for lt in language_translations {
                        map.insert(lt.asset_name.clone(), lt);
                        println!("DB Updated @ {:?}", std::time::SystemTime::now());
                        println!("DB Updated Data: {:?}", map);
                    }
                }
                Err(_) => {
                    println!("Write lock is not available yet, retrying...");
                    continue;
                }
            }

            thread::sleep(Duration::from_secs(1));
        }
    });


    App::new()
        .add_plugins(DefaultPlugins)
        .insert_resource(LanguageTranslationResource::new(resource_map_clone))
        .add_systems(Startup, setup)
        .add_systems(Update, bevy::window::close_on_esc)
        .add_systems(Update, text_color_system)
        .add_systems(Update, update_translation_system)
        .run();

    Ok(())
}

#[derive(Resource)]
struct LanguageTranslationResource {
    database: Arc<RwLock<HashMap<String, LanguageTranslation>>>,
}

impl LanguageTranslationResource {
    fn new(database: Arc<RwLock<HashMap<String, LanguageTranslation>>>) -> Self {
        Self {
            database
        }
    }
}

#[derive(Debug)]
struct LanguageTranslation {
    language_name: String,
    asset_name: String,
    translation: String,
}

impl LanguageTranslation {
    fn new(language_name: String, asset_name: String, translation: String) -> Self {
        LanguageTranslation {
            language_name,
            asset_name,
            translation,
        }
    }
}

fn poll(connection: &mut Connection) -> Vec<LanguageTranslation> {
    let mut statement = connection.prepare(query).unwrap();
    let translations: Vec<LanguageTranslation> = statement.query_map([], |row| {
        Ok(LanguageTranslation::new(
            row.get_unwrap(0),
            row.get_unwrap(1),
            row.get_unwrap(2)))
    })
        .unwrap()
        .map(|t| t.unwrap())
        .collect();

    translations
}

#[derive(Component)]
struct ColorText;

fn setup(mut commands: Commands, asset_server: Res<AssetServer>) {
    commands.spawn(Camera2dBundle::default());
    commands.spawn((
        // Create a TextBundle that has a Text with a single section.
        TextBundle::from_section(
            // Accepts a `String` or any type that converts into a `String`, such as `&str`
            "Starting Text",
            TextStyle {
                // This font is loaded and will be used instead of the default font.
                font: asset_server.load("fonts/FiraCode-Regular.ttf"),
                font_size: 100.0,
                ..default()
            },
        ) // Set the justification of the Text
            .with_text_justify(JustifyText::Center)
            // Set the style of the TextBundle itself.
            .with_style(Style {
                position_type: PositionType::Absolute,
                bottom: Val::Px(5.0),
                right: Val::Px(5.0),
                ..default()
            }),
        ColorText,
    ));
}

fn update_translation_system(mut q: Query<&mut Text, With<ColorText>>, language_translation_resource: Res<LanguageTranslationResource>) {
    let translations = language_translation_resource.database.read().unwrap();
    
    for mut text in &mut q {
        text.sections[0].value.clone_from(&translations.get("GAME_MAIN_MENU").unwrap().translation)
    }
}

fn text_color_system(time: Res<Time>, mut q: Query<&mut Text, With<ColorText>>) {
    for mut text in &mut q {
        let seconds = time.elapsed_seconds();

        // Update the color of the first and only section.
        text.sections[0].style.color = Color::Rgba {
            red: (1.25 * seconds).sin() / 2.0 + 0.5,
            green: (0.75 * seconds).sin() / 2.0 + 0.5,
            blue: (0.50 * seconds).sin() / 2.0 + 0.5,
            alpha: 1.0,
        };
    }
}

