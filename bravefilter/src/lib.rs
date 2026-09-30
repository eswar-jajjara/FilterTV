use adblock::request::Request;
use adblock::{
    Engine, FilterSet,
    lists::{ParseOptions, RuleTypes},
};
use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jboolean;
use std::sync::{OnceLock, RwLock};

static ENGINE: OnceLock<RwLock<Engine>> = OnceLock::new();

fn engine() -> &'static RwLock<Engine> {
    ENGINE.get_or_init(|| RwLock::new(Engine::default()))
}

fn replace_rules(rules: String) -> bool {
    let mut set = FilterSet::new(false);
    set.add_filter_list(
        rules,
        ParseOptions {
            rule_types: RuleTypes::NetworkOnly,
            ..ParseOptions::default()
        },
    );
    let next = Engine::new_with_filter_set(set);
    match engine().write() {
        Ok(mut current) => {
            *current = next;
            true
        }
        Err(_) => false,
    }
}

fn should_block(url: &str, source: &str, request_type: &str, method: &str) -> bool {
    let Ok(request) = Request::new(url, source, request_type, method) else {
        return false;
    };
    engine()
        .read()
        .map(|current| current.check_network_request(&request).should_block())
        .unwrap_or(false)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_liskovsoft_smartyoutubetv2_common_filter_BraveFilterEngine_nativeReplaceRules(
    mut env: JNIEnv,
    _class: JClass,
    rules: JString,
) -> jboolean {
    let Ok(rules) = env.get_string(&rules) else {
        return 0;
    };
    u8::from(replace_rules(rules.into()))
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_liskovsoft_smartyoutubetv2_common_filter_BraveFilterEngine_nativeShouldBlock(
    mut env: JNIEnv,
    _class: JClass,
    url: JString,
    source: JString,
    request_type: JString,
    method: JString,
) -> jboolean {
    let Ok(url) = env.get_string(&url) else {
        return 0;
    };
    let Ok(source) = env.get_string(&source) else {
        return 0;
    };
    let Ok(request_type) = env.get_string(&request_type) else {
        return 0;
    };
    let Ok(method) = env.get_string(&method) else {
        return 0;
    };
    u8::from(should_block(&url, &source, &request_type, &method))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn matches_url_and_exception() {
        assert!(replace_rules(
            "||ads.example.test^\n@@||safe.ads.example.test^".to_string()
        ));
        assert!(should_block(
            "https://ads.example.test/spot",
            "https://www.youtube.com/watch",
            "media",
            "GET"
        ));
        assert!(!should_block(
            "https://safe.ads.example.test/spot",
            "https://www.youtube.com/watch",
            "media",
            "GET"
        ));
        assert!(!should_block(
            "https://video.example.test/watch",
            "https://www.youtube.com/watch",
            "media",
            "GET"
        ));
    }
}
