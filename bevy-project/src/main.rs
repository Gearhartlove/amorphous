use bevy::prelude::*;
use rusqlite::{Connection, Result as SQLiteError};
use std::{thread, time::Duration};

fn main() -> SQLiteError<()> {
    let path = "../sqlite/amorphous";

    thread::spawn(move || {
        let mut connection = Connection::open(path).unwrap();
        loop {
            let _ = poll(&mut connection);
            thread::sleep(Duration::from_secs(1));
        }
    });

    App::new()
        .add_plugins(DefaultPlugins)
        .add_systems(Startup, setup)
        .add_systems(Update, bevy::window::close_on_esc)
        .add_systems(Update, text_color_system)
        .run();

    Ok(())
}

fn poll(connection: &mut Connection) -> SQLiteError<()> {
    let mut statement = connection.prepare("SELECT * FROM user")?;
    let person_iter = statement.query_map([], |row| {
        println!("------------------------------------");
        println!("{:?}", std::time::SystemTime::now());
        println!("{:?}", row);
        Ok(())
    })?;

    for _ in person_iter {
        // print out my rows because the above iterator is lazy of course ;)
    }

    Ok(())
}

#[derive(Component)]
struct ColorText;

fn setup(mut commands: Commands, asset_server: Res<AssetServer>) {
    commands.spawn(Camera2dBundle::default());
    commands.spawn((
        // Create a TextBundle that has a Text with a single section.
        TextBundle::from_section(
            // Accepts a `String` or any type that converts into a `String`, such as `&str`
            "hello\nbevy!",
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

fn text_color_system(time: Res<Time>, mut query: Query<&mut Text, With<ColorText>>) {
    for mut text in &mut query {
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

