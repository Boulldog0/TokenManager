# TokenManager
**Plugin complet permettant de créer une nouvelle monnaie virtuelle autre que le simple argent de vault, pour créer des boutiques personnalisés ou une économie personnalisé.**

**Plugin compatible Vault ( systéme economique ) et PlaceholderAPI ( pour afficher des informations sur le plugin )**

# En cas de suggestion, je serais ravi de vous recevoir sur ce discord : https://discord.gg/ugZ7SPZ85J

**Features principales :**

- Systéme de trade sécurisé token vs argent vault :
Pour effectuer un trade sécurisé, le joueur voulant donner ses tokens ( et recevoir l'argent vault ) lance une requete de trade au joueur voulant obtenir les tokens. Rien de plus simple !
- Systéme de stockage de donnée différé :
Pour pouvoir stocker les données du plugin sur une base de donnée MySQL ou sur un fichier local
- Systéme de logs complet : 
Permet de logger toutes les actions ( commandes admins, pay, trades, echecs de trade, ... ) qui se passe sur le serveur
- Systéme de commandes admins simple et complet : 
Il n'y a que peu de commandes admins, et toutes celles si sont simples d'utilisation.

# La configuration du plugin est simple et rapide a prendre en main.

# Implémentation PlaceHolderAPI : 

**Le plugin est compatible avec PlaceHolderAPI. Le seul placeholder utilisable pour le moment :**
**%token% > Permet d'afficher le nombre de tokens du joueur.**

**Commandes, Description et permission :**

| Commande | Description | Permission |
|----------|----------|----------|
| /token  | Permet de voir son nombre de token.  | tokenmanager.viewtoken | 
| /token pay <joueur> <montant> | Permet de payer des tokens a des joueurs. | tokenmanager.pay | 
| /atoken <give|withdraw|set|view|reset> <joueur> <montant> | Commandes d'administration du plugin | tokenmanager.admintoken |
| /treload | Permet de recharger la configuration du plugin | tokenmanager.reload |
| /ttoken trade <joueurs> <tokens> <money> | Permet d'effectuer des trades sécurisés entre argent vault et tokens | tokenmanager.trade |

# L'entiéreté de la configuration est expliqué dessus, ainsi que toute la liste des placeholders locaux utilisables.

## En cas de besoin de support, je suis joignable tout le temps sur discord : Boulldogo#0001 ou rendez vous sur ce discord : https://discord.gg/ugZ7SPZ85J
