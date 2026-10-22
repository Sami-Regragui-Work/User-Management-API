# Documentation : Spring Core / Spring Data JPA / Spring MVC (sans Spring Boot)

## Spring Core : Inversion de Contrôle et Injection de Dépendances

**Qu'est-ce que Spring Core et à quoi sert-il ?**
C'est le noyau du framework. Il crée les objets de l'application (les beans), les relie entre eux et gère leur cycle de vie. Le code métier reste simple et peu couplé.

**Que signifie l'Inversion de Contrôle (IoC) ?**
Nos classes ne créent plus leurs dépendances avec `new` : c'est le conteneur Spring qui s'en charge. On cède le contrôle de la création, d'où « inversion ».

**Quelle différence entre IoC et DI ?**
L'IoC est le principe. L'injection de dépendances (DI) est la façon de l'appliquer : le conteneur injecte les objets nécessaires, par constructeur, setter ou champ.

**Qu'est-ce qu'un bean ?**
Un objet instancié, configuré et géré par le conteneur Spring.

**Quel est le rôle du conteneur IoC ?**
Il lit la configuration, crée les beans, injecte leurs dépendances et gère leur cycle de vie.

**ApplicationContext ou BeanFactory ?**

| | BeanFactory | ApplicationContext |
|---|---|---|
| Rôle | Conteneur de base | BeanFactory + fonctionnalités en plus |
| Création des beans | À la demande | Singletons créés au démarrage |
| Extras | Aucun | Événements, i18n, AOP, hiérarchie de contextes |

On utilise toujours `ApplicationContext`.

**Les trois approches de configuration ?**

| Approche | Principe |
|---|---|
| XML | Beans déclarés dans un fichier. Ancien et verbeux. |
| Annotations | `@Component` et co., détectés par scan |
| Java Config | Classes `@Configuration` avec des méthodes `@Bean`. Explicite et vérifié à la compilation. |

**À quoi servent ces annotations ?**

| Annotation | Rôle |
|---|---|
| `@Configuration` | Classe qui déclare des beans et de la configuration |
| `@ComponentScan` | Indique les packages à scanner pour trouver les composants |
| `@Bean` | Le retour de la méthode devient un bean (pratique pour les classes tierces) |
| `@Component` | Marque une classe comme bean (stéréotype générique) |
| `@Service` | Bean de la logique métier |
| `@Repository` | Bean d'accès aux données (traduit aussi les exceptions de persistance) |
| `@Controller` | Bean de la couche web |
| `@Autowired` | Injection par type |
| `@Qualifier` | Précise quel bean injecter quand il y en a plusieurs du même type |

**Comment Spring détecte et crée les composants ?**
`@ComponentScan` parcourt les packages indiqués, repère les classes annotées (`@Component` et dérivés), enregistre leur définition, puis les instancie.

**Cycle de vie d'un bean ?**
Instanciation, injection des dépendances, callbacks d'initialisation (`@PostConstruct`, `InitializingBean`), bean prêt à l'emploi, puis à la fermeture du contexte `@PreDestroy`.

**Quelle différence entre les scopes ?**

| Scope | Comportement |
|---|---|
| `singleton` (défaut) | Une seule instance par contexte |
| `prototype` | Une nouvelle instance à chaque demande |
| `request`, `session`, `application` | Scopes web : une instance par requête, session ou application |

**Pourquoi comprendre la configuration manuelle ?**
Spring Boot cache tout derrière l'auto-configuration. En configurant à la main (DataSource, EntityManagerFactory, DispatcherServlet), on sait ce qui est créé et pourquoi, donc on sait déboguer.

---

## Spring Data JPA : persistance et transactions

**Qu'est-ce que Spring Data JPA et quel problème résout-il ?**
Une couche au-dessus de JPA qui génère l'implémentation des repositories à partir d'une simple interface. Fini le code CRUD répétitif.

**Quelle différence entre JPA et Hibernate ?**
JPA est une spécification (une API et des annotations). Hibernate en est l'implémentation la plus utilisée.

**Qu'est-ce qu'une entité JPA ?**
Une classe annotée `@Entity`, mappée sur une table. Chaque instance correspond à une ligne, et `@Id` désigne la clé primaire.

**À quoi sert le DataSource ?**
Il fournit les connexions à la base (URL, utilisateur, mot de passe), en général via un pool comme HikariCP.

**Que fait l'EntityManager ?**
Il persiste, recherche, modifie et supprime les entités. Il gère aussi le contexte de persistance (suivi des modifications et synchronisation avec la base).

**Responsabilité du TransactionManager ?**
Ouvrir une transaction, la valider (commit) ou l'annuler (rollback). `JpaTransactionManager` relie la transaction à l'EntityManager.

**À quoi sert `@EnableJpaRepositories` ?**
Elle scanne les interfaces repository du package indiqué et crée leur implémentation sous forme de beans.

**Qu'est-ce qu'un repository Spring Data ?**
Une interface qui étend `JpaRepository<Entité, TypeId>`. Spring génère l'implémentation, et les requêtes peuvent être déduites du nom de la méthode, par exemple `findByEmailAndActiveTrue`.

**Méthodes génériques de `JpaRepository` ?**
`save`, `saveAll`, `findById`, `findAll` (avec `Pageable` possible), `existsById`, `count`, `delete`, `deleteById`, `flush`, `saveAndFlush`.

**Comment gérer les transactions ?**
On active `@EnableTransactionManagement`, puis on place `@Transactional` sur la couche service.
- **Propagation** : `REQUIRED` par défaut (rejoint la transaction existante), `REQUIRES_NEW` pour en ouvrir une indépendante.
- **Lecture seule** : `readOnly = true` pour les consultations.
- **Rollback** : automatique sur `RuntimeException`, configurable avec `rollbackFor` pour les exceptions contrôlées.

**Pourquoi définir la connexion à la main avant Spring Boot ?**
Sans Boot, rien ne devine la configuration : il faut déclarer soi-même la source de données et la persistance. C'est aussi ce qui permet de comprendre ce que Boot automatise.

**Que contient une configuration de persistance complète ?**
- un `DataSource`
- un `LocalContainerEntityManagerFactoryBean` (packages d'entités, adaptateur Hibernate, propriétés)
- un `JpaTransactionManager`
- `@EnableJpaRepositories` et `@EnableTransactionManagement`

**Qu'est-ce que la validation de contraintes dans le modèle ?**
Des règles déclaratives sur les données.
- **Bean Validation** (`@NotBlank`, `@Email`, `@Size`) sur les DTO, déclenchée par `@Valid`.
- **Contraintes en base** (`@Column(unique = true, nullable = false)`) qui garantissent la règle même si le code la rate. Exemple : l'email unique est protégé par la base, et vérifié en amont dans le service pour renvoyer un 409 clair.

**Suppression logique ou physique ?**

| | Physique | Logique (soft delete) |
|---|---|---|
| Principe | `DELETE`, la ligne disparaît | On marque la ligne (`active = false`) |
| Données | Perdues définitivement | Conservées, réactivables |
| Requêtes | Rien à filtrer | Doivent filtrer les lignes actives |

---

## Spring MVC : contrôleurs et couche web

**Que signifie MVC et quel est son objectif ?**
Modèle (les données), Vue (l'affichage), Contrôleur (reçoit la requête et orchestre). Le but est de séparer les responsabilités. Dans une API REST, la « vue » est le JSON.

**Rôle du DispatcherServlet ?**
C'est le point d'entrée unique (front controller). Toutes les requêtes passent par lui : il trouve le bon contrôleur, l'appelle et renvoie la réponse.

**Controller ou RestController ?**
`@Controller` renvoie un nom de vue (JSP, par exemple). `@RestController` = `@Controller` + `@ResponseBody` : il renvoie directement les données, sérialisées en JSON.

**À quoi servent ces annotations ?**

| Annotation | Rôle |
|---|---|
| `@RequestMapping` | Associe une URL et une méthode HTTP à une classe ou une méthode (au niveau de la classe, c'est un préfixe) |
| `@GetMapping` | Lire |
| `@PostMapping` | Créer |
| `@PutMapping` | Modifier |
| `@DeleteMapping` | Supprimer |
| `@Valid` | Déclenche la validation de l'objet. En cas d'échec : `MethodArgumentNotValidException` |
| `@RequestBody` | Convertit le corps JSON de la requête en objet |
| `@PathVariable` | Récupère une valeur de l'URL, comme `{id}` |

**Comment le DispatcherServlet traite une requête, du début à la fin ?**
1. La requête arrive.
2. Le `HandlerMapping` trouve le contrôleur et la méthode.
3. Les intercepteurs s'exécutent (`preHandle`).
4. Les arguments sont résolus : `@PathVariable`, `@RequestBody` désérialisé puis validé.
5. La méthode du contrôleur s'exécute et appelle le service.
6. Le résultat est sérialisé en JSON par un `HttpMessageConverter`.
7. La réponse part, avec son code HTTP.

En cas d'exception, c'est un `@ExceptionHandler` qui construit la réponse d'erreur.

**Qu'est-ce que `WebConfig` et à quoi sert-elle ?**
Une classe `@Configuration` avec `@EnableWebMvc`. Elle active Spring MVC (conversion JSON, validation), indique où scanner les contrôleurs et enregistre les intercepteurs (via `WebMvcConfigurer`).

**Pourquoi initialiser le DispatcherServlet à la main ?**
Spring Boot l'enregistre tout seul. Sans lui, personne ne crée le `DispatcherServlet` ni ne le déclare auprès de Tomcat : c'est à nous de le faire.

**Qu'est-ce qu'un WebAppInitializer et pourquoi remplace-t-il `web.xml` ?**
Une classe qui implémente `WebApplicationInitializer`. Tomcat la détecte au démarrage et appelle `onStartup`. On y crée les contextes Spring et on enregistre le `DispatcherServlet` en Java, ce que faisait avant le fichier XML `web.xml`.

**Étapes de traitement d'une requête REST ?**
Mapping vers le contrôleur, désérialisation et validation du corps, appel du service, construction de la réponse (code HTTP + JSON), et si une erreur survient, passage par le `@RestControllerAdvice`.

**Comment se font la sérialisation et la désérialisation JSON ?**
Par Jackson, via un `HttpMessageConverter`. `@RequestBody` transforme le JSON reçu en objet (désérialisation), et la valeur retournée est transformée en JSON (sérialisation). Les dates `java.time` demandent le module `jackson-datatype-jsr310`.

**À quoi sert `@RestControllerAdvice` ?**
À centraliser la gestion des erreurs : une seule classe, avec des méthodes `@ExceptionHandler`, répond pour tous les contrôleurs. Les erreurs ont ainsi le même format partout, sans dupliquer le code.

**Bonnes pratiques d'organisation des packages ?**
Un package par couche : `config`, `controller`, `service`, `repository`, `model` (entités), `dto`, `mapper`, `exception`.
- Les dépendances vont dans un seul sens : controller, puis service, puis repository.
- L'API expose des DTO, jamais les entités.
- Le contexte racine porte les services et repositories, le contexte web porte les contrôleurs.