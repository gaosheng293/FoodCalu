pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 1. 阿里云公共仓库
        maven {
            url = uri("http://maven.aliyun.com/repository/public")
            isAllowInsecureProtocol = true // <--- 改成了 isAllowInsecureProtocol
        }

        // 2. 阿里云 Google 镜像
        maven {
            url = uri("http://maven.aliyun.com/repository/google")
            isAllowInsecureProtocol = true // <--- 改成了 isAllowInsecureProtocol
        }

        // 3. 阿里云 Gradle 插件镜像
        maven {
            url = uri("http://maven.aliyun.com/repository/gradle-plugin")
            isAllowInsecureProtocol = true // <--- 改成了 isAllowInsecureProtocol
        }

        google()
        mavenCentral()
    }
}

rootProject.name = "FoodCalu2"
include(":app")